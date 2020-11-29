package dev.jhseo.mogj.server.controller

import dev.jhseo.hasher.HasherMethod
import dev.jhseo.hasher.hash
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.model.User
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.lang3.RandomStringUtils
import java.lang.IllegalStateException
import java.lang.NumberFormatException

fun Route.userRoutes() {
    route("/users") {
        get("/{id}") {
            val id: Int
            val user: User
            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                user = User(id) // IllegalStateException by check
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            }

            call.response.status(HttpStatusCode.OK)
            call.respond(
                mapOf(
                    "nickname" to user.nickname,
                    "avatar" to user.avatar,
                    "isParent" to user.isParent,
                    "isChild" to user.isChild,
                    "myHouse" to user.house,
                    "point" to user.point
                )
            )
        }
        get("/{id}/token") {
            val params = call.request.queryParameters
            val id: Int
            val user: User
            val password: String
            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                user = User(id) // IllegalStateException by check
                password = params["password"]!! // NullPointerException
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            }catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            }
            call.response.status(HttpStatusCode.OK)
            call.respond(
                mapOf(
                    "token" to user.regenToken(password)
                )
            )
        }
        get("/{id}/is_valid") {
            val params = call.request.queryParameters
            val id: Int
            val user: User
            val token: String
            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                user = User(id) // IllegalStateException by check
                token = params["token"]!! // NullPointerException
                user.auth(token) // AuthFailException
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.OK)
                call.respond(mapOf("valid" to false))
            }
            call.response.status(HttpStatusCode.OK)
            call.respond(mapOf("valid" to true))
        }
        post("/create") {
            val params = call.receiveParameters()
            val kakaoId: Long
            val nickname: String
            val avatar: String
            val password: String

            try {
                kakaoId = params["kakaoId"]!!.toLong() // NumberFormatException, NullPointerException
                nickname = params["nickname"]!! // NullPointerException
                avatar = params["avatar"]!! // NullPointerException
                password = params["password"]!!.hash(HasherMethod.SHA512) // NullPointerException
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }

            val token = RandomStringUtils.randomAlphabetic(20)
            User.write(kakaoId, nickname, avatar, password, token.hash(HasherMethod.SHA512))
            call.response.status(HttpStatusCode.OK)
            call.respond(
                mapOf(
                    "token" to token
                )
            )
        }
        patch("/{id}") {
            val params = call.receiveParameters()
            val id: Int
            val user: User
            val token: String

            val nickname: String?
            val avatar: String?
            val password: String?
            val isParent: Boolean?
            val isChild: Boolean?
            val house: Int?

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                user = User(id) // IllegalStateException by check

                token = params["token"]!! // NullPointerException
                user.auth(token) // AuthFailException

                nickname = params["nickname"]
                avatar = params["avatar"]
                password = params["password"]?.hash(HasherMethod.SHA512)
                isParent = params["isParent"]?.toBoolean()
                isChild = params["isChild"]?.toBoolean()
                house = params["house"]?.toInt() // NumberFormatException
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@patch
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@patch
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@patch
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@patch
            }

            user.update(Users.nickname, nickname)
                .update(Users.avatar, avatar)
                .update(Users.password, password)
                .update(Users.isParent, isParent)
                .update(Users.isChild, isChild)
                .update(Users.house, house)
            call.response.status(HttpStatusCode.OK)
        }
    }
}