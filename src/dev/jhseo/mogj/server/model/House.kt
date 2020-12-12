package dev.jhseo.mogj.server.model

import dev.jhseo.mogj.server.NotParentException
import dev.jhseo.mogj.server.db.Houses
import dev.jhseo.mogj.server.db.ParentsHouses
import dev.jhseo.mogj.server.db.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class House(val id: Int) {
    @Transient private val exposedObj: Query

    val name: String
    val avatar: String?
    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime
    val parents: List<Int>
    val members: List<Int>

    init {
        exposedObj = Houses.select { Houses.id eq id }
        check(transaction { exposedObj.count() } != 0L)
        name = transaction { exposedObj.map { it[Houses.name] }.first() }
        avatar = transaction { exposedObj.map { it[Houses.avatar] }.firstOrNull() }
        createdAt = transaction { exposedObj.map { it[Houses.created_at] }.first() }
        updatedAt = transaction { exposedObj.map { it[Houses.updated_at] }.first() }
        parents = transaction {
            ParentsHouses.select { ParentsHouses.houseId eq this@House.id }.map { it[ParentsHouses.parentId] }
        }
        members = transaction {
            Users.select { Users.house eq this@House.id }.map { it[Users.id].value }
        }
    }

    constructor (query: Query) : this(
        transaction {
            query.map { it[Houses.id].value }.first()
        }
    )

    fun <T> update(column: Column<T>, to: T?): House {
        val myId = this.id
        if (to != null) {
            transaction {
                Houses.update({ Houses.id eq myId }) { it[column] = to; it[Houses.updated_at] = LocalDateTime.now() }
            }
        }
        return House(myId)
    }

    fun addParent(parentId: Int) {
        val myId = this.id
        if (!User(parentId).isParent) throw NotParentException()
        transaction {
            ParentsHouses.insert {
                it[houseId] = myId
                it[ParentsHouses.parentId] = parentId
                it[created_at] = LocalDateTime.now()
                it[updated_at] = LocalDateTime.now()
            }
        }
    }

    companion object {
        fun write(name: String, avatar: String?): House {
            return House(transaction {
                Houses.insertAndGetId {
                    it[Houses.name] = name
                    it[Houses.avatar] = avatar
                    it[created_at] = LocalDateTime.now()
                    it[updated_at] = LocalDateTime.now()
                }.value
            })
        }
    }
}