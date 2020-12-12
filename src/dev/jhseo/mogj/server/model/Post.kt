package dev.jhseo.mogj.server.model

import com.google.gson.annotations.SerializedName
import dev.jhseo.mogj.server.db.*
import dev.jhseo.mogj.server.procedures.revealDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class Post(val id: Int) {
    @Transient
    private val exposedObj: Query

    val writer: Int
    val receiver: Int
    val title: String
    val content: String
    val image: String?
    val likes: Int
    val likers: List<Int>
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    init {
        exposedObj = Posts.select { Posts.id eq id }
        check(transaction { exposedObj.count() } != 0L)

        writer = transaction { exposedObj.map { it[Posts.writer] }.first() }
        receiver = transaction { exposedObj.map { it[Posts.receiver] }.first() }
        title = transaction { exposedObj.map { it[Posts.title] }.first() }
        content = transaction { exposedObj.map { it[Posts.content] }.first() }
        image = if (transaction { exposedObj.map { it[Posts.image_extension] }.first() } != null) {
            "https://images.jhseo.dev/mogj/posts/${id}.${transaction { exposedObj.map { it[Posts.image_extension] }.first() }}"
        } else null
        likes = transaction { exposedObj.map { it[Posts.likes] }.first() }
        likers = transaction {
            PostsUsersLikes.select { PostsUsersLikes.postId eq this@Post.id }.map { it[PostsUsersLikes.userId] }
        }
        createdAt = transaction { exposedObj.map { it[Posts.created_at] }.first() }
        updatedAt = transaction { exposedObj.map { it[Posts.updated_at] }.first() }
    }

    constructor (query: Query) : this(
        transaction {
            query.map { it[Posts.id].value }.first()
        }
    )

    fun <T> update(column: Column<T>, to: T?): Post {
        val myId = this.id
        if (to != null) {
            transaction {
                Posts.update({ Posts.id eq myId }) { it[column] = to; it[updated_at] = LocalDateTime.now() }
            }
        }
        return Post(myId)
    }

    fun like(user: User) {
        val myId = id
        val didLike: Boolean = transaction {
            PostsUsersLikes.select((PostsUsersLikes.userId eq user.id) and (PostsUsersLikes.postId eq myId)).count()
        } != 0L
        if (didLike) {
            update(Posts.likes, likes - 1)
            transaction {
                PostsUsersLikes.deleteWhere {
                    PostsUsersLikes.userId eq user.id
                }
            }
        } else {
            update(Posts.likes, likes + 1)
            transaction {
                PostsUsersLikes.insert {
                    it[userId] = user.id
                    it[postId] = myId
                }
            }
        }
    }

    fun comments(from: Long): List<Comment> {
        val myId = id
        return transaction {
            Comments.select {
                Comments.parent eq myId
            }.orderBy(Comments.created_at, SortOrder.DESC)
                .limit(30, offset = from)
                .map {
                    Comment(it[Comments.id].value)
                }
        }
    }

    companion object {
        fun write(writer: Int, receiver: Int, title: String, content: String): Post {
            return Post(transaction {
                Posts.insertAndGetId {
                    it[Posts.writer] = writer
                    it[Posts.receiver] = receiver
                    it[Posts.title] = title
                    it[Posts.content] = content
                    it[created_at] = LocalDateTime.now()
                    it[updated_at] = LocalDateTime.now()
                }.value
            })
        }

        fun viewablePosts(user: User, from: Long): List<Post> {
            val houseId = user.house

            val query: Query
            if (user.isParent) {
                val houses = transaction {
                    ParentsHouses.select {
                        ParentsHouses.parentId eq user.id
                    }.map { it[ParentsHouses.houseId] }
                }
                query = if (user.revealTime != null) {
                    Posts.select {
                        ((Posts.receiver inList houses) or (Posts.writer inList houses)) and (Posts.created_at less revealDateTime(user)!!)
                    }
                } else {
                    Posts.select {
                        (Posts.receiver inList houses) or (Posts.writer inList houses)
                    }
                }
            } else if (user.isChild && user.house != -1) {
                val parents = House(houseId).parents
                query = Posts.select {
                    (Posts.receiver inList parents) or (Posts.writer inList parents)
                }
            } else {
                return emptyList()
            }

            return transaction {
                query.orderBy(Posts.created_at to SortOrder.DESC)
                    .limit(30, offset = from)
                    .map { Post(it[Posts.id].value) }
            }
        }
    }

}