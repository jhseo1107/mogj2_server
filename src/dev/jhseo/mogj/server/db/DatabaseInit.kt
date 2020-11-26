package dev.jhseo.mogj.server.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.mariadb.jdbc.Driver

fun initDb() {
    Database.connect(
        url = "jdbc:mysql://localhost:3306/mogj?serverTimeZone=Asia/Seoul&characterEncoding=utf8&useUnicode=true",
        driver = Driver::class.java.name,
        user = "dockhyub",
        password = System.getenv("DBPASSWORD")
    )

    transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.let {
            it.create(Users)
        }
    }
}