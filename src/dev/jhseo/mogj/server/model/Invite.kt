package dev.jhseo.mogj.server.model

import dev.jhseo.mogj.server.db.Invites
import dev.jhseo.mogj.server.db.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class Invite(val id: Int) {
    @Transient private val exposedObj: Query

    val issuer: Int
    val isUsed: Boolean
    val expiredAt: LocalDateTime

    init {
        exposedObj = Invites.select { Invites.id eq id }
        check(transaction { exposedObj.count() } != 0L)

        issuer = transaction { exposedObj.map { it[Invites.issuer] }.first() }
        isUsed = transaction { exposedObj.map { it[Invites.isUsed] }.first() }
        expiredAt = transaction { exposedObj.map { it[Invites.expiredAt] }.first() }
    }

    constructor (query: Query) : this(
        transaction {
            query.map { it[Invites.id].value }.first()
        }
    )

    fun <T> update(column: Column<T>, to: T?): Invite {
        val myId = this.id
        if (to != null) {
            transaction {
                Invites.update({ Invites.id eq myId }) { it[column] = to }
            }
        }
        return Invite(myId)
    }

    companion object {
        fun write(issuer: Int, expiredAt: LocalDateTime) : Invite {
            return Invite(transaction {
                Invites.insertAndGetId {
                    it[Invites.issuer] = issuer
                    it[Invites.expiredAt] = expiredAt
                }.value
            })
        }
    }
}