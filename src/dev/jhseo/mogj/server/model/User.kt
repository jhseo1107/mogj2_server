package dev.jhseo.mogj.server.model

import dev.jhseo.hasher.HasherMethod
import dev.jhseo.hasher.hash
import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.NotAccessibleException
import dev.jhseo.mogj.server.db.ConnectedParentsHouses
import dev.jhseo.mogj.server.db.ParentsHouses
import dev.jhseo.mogj.server.db.Posts
import dev.jhseo.mogj.server.db.Users
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.LocalTime

data class User(val id: Int) {
    @Transient private val exposedObj: Query

    @Transient val kakaoId: Long
    val nickname: String
    val avatar: String
    @Transient val password: String
    @Transient val token: String
    val isParent: Boolean
    val isChild: Boolean
    val house: Int
    val point: Int
    val revealTime: LocalTime?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    init {
        exposedObj = Users.select { Users.id eq id }
        check(transaction { exposedObj.count() } != 0L)

        kakaoId = transaction { exposedObj.map { it[Users.kakaoId] }.first() }
        nickname = transaction { exposedObj.map { it[Users.nickname] }.first() }
        avatar = transaction { exposedObj.map { it[Users.avatar] }.first() }
        password = transaction { exposedObj.map { it[Users.password] }.first() }
        token = transaction { exposedObj.map { it[Users.token] }.first() }
        isParent = transaction { exposedObj.map { it[Users.isParent] }.first() }
        isChild = transaction { exposedObj.map { it[Users.isChild] }.first() }
        house = transaction { exposedObj.map { it[Users.house] }.firstOrNull() } ?: -1
        point = transaction { exposedObj.map { it[Users.point] }.first() }
        revealTime = transaction {exposedObj.map{it[Users.revealTime]}.firstOrNull()?.toLocalTime()}
        createdAt = transaction { exposedObj.map { it[Users.created_at] }.first() }
        updatedAt = transaction { exposedObj.map { it[Users.updated_at] }.first() }
    }

    constructor (query: Query) : this(
        transaction {
            query.map { it[Users.id].value }.firstOrNull()!!
        }
    )

    fun <T> update(column: Column<T>, to: T?): User {
        val myId = this.id
        if (to != null) {
            transaction {
                Users.update({ Users.id eq myId }) { it[column] = to; it[updated_at] = LocalDateTime.now() }
            }
        } else if (column == Users.house) {
            transaction {
                Users.update({ Users.id eq myId }) { it[Users.house] = to; it[updated_at] = LocalDateTime.now() }
            }
        }
        return User(myId)
    }

    fun auth(token: String) {
        if (token.hash(HasherMethod.SHA512) != this.token) throw AuthFailException()
    }

    fun regenToken(password: String): String {
        if (password.hash(HasherMethod.SHA512) != this.password) throw AuthFailException()
        val token = RandomStringUtils.randomAlphabetic(20)
        update(Users.token, token.hash(HasherMethod.SHA512))
        return token
    }

    fun checkAccess(userId: Int) {
        val myId = id
        if (transaction {
                ConnectedParentsHouses.select {
                    (((ConnectedParentsHouses.houseId eq house) and (ConnectedParentsHouses.parentId eq userId)) // Writer is house member
                            or ((ConnectedParentsHouses.parentId eq myId) and (ConnectedParentsHouses.houseId eq User(
                        userId
                    ).house))) // Writer is parent
                }.count()
            } == 0L) throw NotAccessibleException()
    }

    fun checkAccess(post: Post) {
        if(isParent) {
            val houses = transaction { ParentsHouses.select {
                ParentsHouses.parentId eq this@User.id
            }.map {it[ParentsHouses.houseId]} }
            if(!(post.writer in houses || post.receiver in houses)) {
                throw NotAccessibleException()
            }
        }
        // Writer is house member
        // Receiver is house member == Writer is parent
        // Writer is parent
        // Receiver is parent
        else if(isChild) {
            if (!(User(post.writer).house == house || post.writer in House(house).parents || post.receiver in House(house).parents)) {
                throw NotAccessibleException()
            }
        }
        else {
            throw NotAccessibleException()
        }
    }

    companion object {
        fun write(kakaoId: Long, nickname: String, avatar: String, password: String, token: String): User {
            return User(transaction {
                Users.insertAndGetId {
                    it[Users.kakaoId] = kakaoId
                    it[Users.nickname] = nickname
                    it[Users.avatar] = avatar
                    it[Users.password] = password
                    it[Users.token] = token
                    it[created_at] = LocalDateTime.now()
                    it[updated_at] = LocalDateTime.now()
                }.value
            })
        }
    }
}
