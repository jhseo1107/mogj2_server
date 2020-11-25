package dev.jhseo.mogj.server

import dev.jhseo.mogj.server.controller.userRoutes
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.content.*
import io.ktor.sessions.*
import io.ktor.features.*
import io.ktor.gson.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    routing {
        get("/") {
            call.respond(mapOf("android" to "1.0.0"))
        }

        static("/static") {
            resources("static")
        }

        // Controllers from dev.jhseo.mogj.server.controller
        userRoutes()
    }
}

data class MySession(val count: Int = 0)

