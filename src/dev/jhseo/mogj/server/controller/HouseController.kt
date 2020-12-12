package dev.jhseo.mogj.server.controller

import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.NotParentException
import dev.jhseo.mogj.server.db.Houses
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.model.House
import dev.jhseo.mogj.server.model.User
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.houseRoutes() {
    route("/houses") {
        get("/{id}") {
            val id: Int
            val house: House

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                house = House(id) // IllegalStateException by check
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            }

            call.response.status(HttpStatusCode.OK)
            call.respond(house)
        }
        patch("/{id}") {
            val params = call.receiveParameters()
            val id: Int
            val house: House

            val userId: Int
            val userToken: String

            val name: String?
            val avatar: String?

            val user: User

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                house = House(id) // IllegalStateException by check
                userId = params["userId"]!!.toInt() // NumberFormatException, NullPointerException
                userToken = params["userToken"]!! // NullPointerException

                user = User(userId) // ISE by check
                user.auth(userToken) // AuthFailException

                name = params["name"]
                avatar = params["name"]
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
            if(user.house != id) {
                call.response.status(HttpStatusCode.Forbidden)
                return@patch
            }

            house.update(Houses.name, name)
                 .update(Houses.avatar, avatar)
            call.response.status(HttpStatusCode.OK)
        }
        post("/create") {
            val params = call.receiveParameters()

            val id: Int
            val token: String

            val user: User
            val house: House

            try {
                id = params["id"]!!.toInt()
                token = params["token"]!!

                user = User(id)
                user.auth(token)
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
            if(!user.isChild) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }
            if(user.house != -1) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }

            house = House.write(user.nickname, user.avatar)
            user.update(Users.house, house.id)
            call.response.status(HttpStatusCode.OK)
        }
        post("/leave") {
            val params = call.receiveParameters()

            val id: Int
            val token: String

            val user: User
            val house: House

            try {
                id = params["id"]!!.toInt()
                token = params["token"]!!

                user = User(id)
                user.auth(token)
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
            if(!user.isChild) {
                call.response.status(HttpStatusCode.BadRequest)
                return@post
            }

            user.update(Users.house, null)
        }
    }
}