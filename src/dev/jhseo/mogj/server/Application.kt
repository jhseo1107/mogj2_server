package dev.jhseo.mogj.server

import dev.jhseo.mogj.server.controller.houseRoutes
import dev.jhseo.mogj.server.controller.userRoutes
import dev.jhseo.mogj.server.db.initDb
import dev.jhseo.mogj.server.procedures.PrntMtchRunner
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.content.*
import io.ktor.sessions.*
import io.ktor.features.*
import io.ktor.gson.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    initDb()
    GlobalScope.launch {
        //Run ParentMatcher Next Monday 09:00
        val localDate = LocalDate.now()
        val nextMonday = localDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val nextMondayMorning = nextMonday.atTime(9, 0)
        println("NextMonday: $nextMonday")
        println("NextMondayMorning: $nextMondayMorning")
        println("Delayed: ${ChronoUnit.MILLIS.between(LocalDateTime.now(), nextMondayMorning)}")
        delay(ChronoUnit.MILLIS.between(LocalDateTime.now(), nextMondayMorning))
        PrntMtchRunner.run()
    }

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
        houseRoutes()
    }
}

data class MySession(val count: Int = 0)

