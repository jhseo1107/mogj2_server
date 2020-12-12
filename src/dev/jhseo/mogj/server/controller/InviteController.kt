package dev.jhseo.mogj.server.controller

import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.db.Invites
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.model.House
import dev.jhseo.mogj.server.model.Invite
import dev.jhseo.mogj.server.model.User
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import java.time.LocalDateTime

fun Route.inviteRoutes() {
    route("/invites") {
        get("/{id}") { // Call from humans
            call.respondRedirect("https://play.google.com/store/apps/details?id=dev.jhseo.mogj.android")
        }

        post("/{id}") { // Internal call from mobile application
            val params = call.receiveParameters()
            val id: Int
            val invite: Invite
            val user: User
            val userId: Int
            val token: String

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                invite = Invite(id) // IllegalStateException by check

                token = params["token"]!! // NullPointerException
                userId = params["userId"]!!.toInt()
                user = User(userId).also { it.auth(token) } // AuthFailException
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@post
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@post
            }

            if(invite.expiredAt < LocalDateTime.now()) {
                call.response.status(HttpStatusCode.Conflict)
                return@post
            }
            if(invite.isUsed) {
                call.response.status(HttpStatusCode.Conflict)
                return@post
            }

            val house = House(User(invite.issuer).house)
            if(user.isParent) {
                if(userId !in house.parents) {
                    house.addParent(userId)
                }
            }
            if(user.isChild) {
                user.update(Users.house, house.id)
            }

            invite.update(Invites.isUsed, true)
            call.response.status(HttpStatusCode.OK)
        }

        post("/create") {
            val params = call.receiveParameters()
            val id: Int
            val invite: Invite
            val user: User
            val token: String

            try {
                id = params["id"]!!.toInt() // NumberFormatException

                token = params["token"]!! // NullPointerException
                user = User(id).also { it.auth(token) } // AuthFailException, IllegalStateException by check
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@post
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@post
            }
            if(user.isParent) {
                call.response.status(HttpStatusCode.BadRequest)
            }

            invite = Invite.write(user.id, LocalDateTime.now().plusDays(3L))

            call.response.status(HttpStatusCode.OK)
            call.respond(mapOf(
                "invite" to "/invites/${invite.id}"
            ))
        }
    }
}