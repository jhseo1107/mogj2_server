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
    val revealTime = datetime(name = "reveal_time").nullable() //But we will only use time

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}

object Houses : IntIdTable(name = "houses") {
    val name = text(name = "name")
    val avatar = text(name = "avatar").nullable()

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}

object ParentsHouses : IntIdTable(name = "parents_houses") { //For Parents Only
    val parentId = integer(name = "parent_id")
    val houseId = integer(name = "house_id")

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}

object ConnectedParentsHouses : IntIdTable(name = "connected_parents_houses") {
    val parentId = integer(name = "parent_id")
    val houseId = integer(name = "house_id")
}

object Invites : IntIdTable(name = "invites") {
    val issuer = integer(name = "issuer")
    val isUsed = bool(name = "is_used").default(false)
    val expiredAt = datetime(name = "expired_at")
}

object Posts : IntIdTable(name = "posts") {
    val writer = integer(name = "writer")
    val receiver = integer(name = "receiver")

    val title = text(name = "title")
    val content = text(name = "content")
    val image_extension = text(name = "image_extension").nullable()
    val likes = integer(name = "likes").default(0)

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}

object Comments : IntIdTable(name = "comments") {
    val writer = integer(name = "writer")
    val parent = integer(name = "parent")

    val content = text(name = "content")

    val created_at = datetime(name = "created_at")
    val updated_at = datetime(name = "updated_at")
}

object PostsUsersLikes : IntIdTable(name = "posts_users_likes") {
    val postId = integer(name = "post_id")
    val userId = integer(name = "user_id")
}