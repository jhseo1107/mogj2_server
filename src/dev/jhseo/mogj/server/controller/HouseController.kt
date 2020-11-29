package dev.jhseo.mogj.server.controller

import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.NotParentException
import dev.jhseo.mogj.server.db.Houses
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
            call.respond(
                mapOf(
                    "name" to house.name,
                    "avatar" to house.avatar,
                    "members" to house.members(),
                    "parents" to house.parents()
                )
            )
        }
        patch("/{id}") {
            val params = call.receiveParameters()
            val id: Int
            val house: House

            val userId: Int
            val userToken: String

            val name: String?
            val avatar: String?

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                house = House(id) // IllegalStateException by check
                userId = params["userId"]!!.toInt() // NumberFormatException, NullPointerException
                userToken = params["userToken"]!! // NullPointerException

                User(userId).auth(userToken) // AuthFailException

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

            house.update(Houses.name, name)
                 .update(Houses.avatar, avatar)
            call.response.status(HttpStatusCode.OK)
        }
        post("/{id}/add_parent") {
            val params = call.receiveParameters()
            val id: Int
            val house: House

            val userId: Int
            val userToken: String

            try {
                id = call.parameters["id"]!!.toInt() // NumberFormatException
                house = House(id) // IllegalStateException by check
                userId = params["userId"]!!.toInt() // NumberFormatException, NullPointerException
                userToken = params["userToken"]!! // NullPointerException

                User(userId).auth(userToken) // AuthFailException
                house.addParent(userId) // NotParentException
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
            } catch (e: NotParentException) {
                call.response.status(HttpStatusCode.Conflict)
                return@post
            }
            call.response.status(HttpStatusCode.OK)
        }
    }
}