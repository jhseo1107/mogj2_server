package dev.jhseo.mogj.server.controller

import dev.jhseo.hasher.HasherMethod
import dev.jhseo.hasher.hash
import dev.jhseo.mogj.server.model.User
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.routing.*

fun Route.userRoutes() {
    route("/users") {
        post("/create") {
            val params = call.receiveParameters()

            val kakaoId: Long = params["kakaoid"]!!.toLong()
            val nickname: String = params["nickname"]!!
            val avatar: String = params["avatar"]!!
            val password: String = params["password"]!!

            User.write(kakaoId, nickname, avatar, password)
        }
    }
}