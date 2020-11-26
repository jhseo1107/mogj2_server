package dev.jhseo.mogj.server.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Users : IntIdTable(name = "users") {
    val kakaoId = long(name = "kakao_id")
    val nickname = text(name = "nickname")
    val avatar = text(name = "avatar")
    val password = text(name = "password")
    val token = text(name = "token")
    val isParent = bool(name = "is_parent").default(false)
    val isChild = bool(name = "is_child").default(false)
    val house = integer(name = "house").nullable()
    val point = integer(name = "point").default(0)

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}