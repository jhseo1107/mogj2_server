package dev.jhseo.mogj.server.model

import dev.jhseo.mogj.server.db.Comments
import dev.jhseo.mogj.server.db.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class Comment(val id: Int) {
    @Transient private val exposedObj: Query

    val writer: Int
    val parent: Int
    val content: String
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    init {
        exposedObj = Comments.select { Comments.id eq id }
        check(transaction { exposedObj.count() } != 0L)

        writer = transaction { exposedObj.map { it[Comments.writer] }.first() }
        parent = transaction { exposedObj.map { it[Comments.parent] }.first() }
        content = transaction { exposedObj.map { it[Comments.content] }.first() }
        createdAt = transaction { exposedObj.map { it[Comments.created_at] }.first() }
        updatedAt = transaction { exposedObj.map { it[Comments.updated_at] }.first() }
    }

    fun <T> update(column: Column<T>, to: T?): Comment {
        val myId = this.id
        if (to != null) {
            transaction {
                Comments.update({ Comments.id eq myId }) { it[column] = to; it[updated_at] = LocalDateTime.now() }
            }
        }
        return Comment(myId)
    }

    companion object {
        fun write(writer: Int, parent: Int, content: String): Comment {
            return Comment(transaction{
                Comments.insertAndGetId {
                    it[Comments.writer] = writer
                    it[Comments.parent] = parent
                    it[Comments.content] = content
                    it[created_at] = LocalDateTime.now()
                    it[updated_at] = LocalDateTime.now()
                }.value
            })
        }
    }
}