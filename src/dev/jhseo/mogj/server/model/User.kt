package dev.jhseo.mogj.server.model

import dev.jhseo.hasher.HasherMethod
import dev.jhseo.hasher.hash
import dev.jhseo.mogj.server.db.Users
import org.apache.commons.lang3.RandomStringUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.lang.RuntimeException
import java.time.LocalDateTime

class User(val id: Int) {
    private val exposedObj: Query

    val kakaoId: Long
    val nickname: String
    val avatar: String
    val password: String
    val token: String
    val isParent: Boolean
    val isChild: Boolean
    val house: Int
    val point: Int

    init {
        exposedObj = Users.select { Users.id eq id }
        check(transaction { exposedObj.count() } != 0.toLong())
        kakaoId = transaction { exposedObj.map { it[Users.kakaoId] }.first() }
        nickname = transaction { exposedObj.map { it[Users.nickname] }.first() }
        avatar = transaction { exposedObj.map { it[Users.avatar] }.first() }
        password = transaction { exposedObj.map { it[Users.password] }.first() }
        token = transaction { exposedObj.map { it[Users.token] }.first() }
        isParent = transaction { exposedObj.map { it[Users.isParent] }.first() }
        isChild = transaction { exposedObj.map { it[Users.isChild] }.first() }
        house = transaction { exposedObj.map { it[Users.house] }.first() } ?: -1
        point = transaction { exposedObj.map { it[Users.point] }.first() }
    }

    constructor (query: Query) : this(
        transaction {
            query.map { it[Users.id].value }.first()
        }
    )

    fun <T> update(column: Column<T>, to: T?): User {
        val myId = this.id
        if (to != null) {
            transaction {
                Users.update({ Users.id eq myId }) { it[column] = to; it[Users.updated_at] = LocalDateTime.now() }
            }
        }
        return User(myId)
    }

    fun auth(token: String) {
        if(token.hash(HasherMethod.SHA512) != this.token) throw AuthFailException()
    }

    fun regenToken(password: String) : String {
        if(password.hash(HasherMethod.SHA512) != this.password) throw AuthFailException()
        val token = RandomStringUtils.randomAlphabetic(20)
        update(Users.password, token.hash(HasherMethod.SHA512))
        return token
    }

    companion object {
        fun write(kakaoId: Long, nickname: String, avatar: String, password: String, token: String): User {
            transaction {
                Users.insert {
                    it[Users.kakaoId] = kakaoId
                    it[Users.nickname] = nickname
                    it[Users.avatar] = avatar
                    it[Users.password] = password
                    it[Users.token] = token
                    it[Users.created_at] = LocalDateTime.now()
                    it[Users.updated_at] = LocalDateTime.now()
                }
            }
            return User(Users.select { Users.kakaoId eq kakaoId })
        }
    }
}

class AuthFailException : RuntimeException()