package dev.jhseo.mogj.server.controller

import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.NotAccessibleException
import dev.jhseo.mogj.server.db.Posts
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.model.Post
import dev.jhseo.mogj.server.model.User
import dev.jhseo.mogj.server.procedures.revealDateTime
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.io.File
import java.io.InputStream
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.lang.NumberFormatException

fun Route.postRoutes() {
    route("/posts") {
        get("") {
            val params = call.request.queryParameters

            val id: Int
            val token: String
            val from: Long

            val user: User
            try {
                id = params["id"]!!.toInt() // NPE, NFE
                token = params["token"]!! //NPE
                from = params["from"]?.toLongOrNull() ?: 0L
                user = User(id).also { it.auth(token) } // AFE, ISE by check
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            }

            call.response.status(HttpStatusCode.OK)
            call.respond(Post.viewablePosts(user, from))
        }
        get("/{id}") {
            val params = call.request.queryParameters
            val userId: Int
            val token: String
            val id: Int

            val user: User
            val post: Post
            try {
                id = call.parameters["id"]!!.toInt() // NPE, NFE
                userId = params["userId"]!!.toInt() // NPE, NFE
                token = params["token"]!! // NPE
                user = User(userId).also { it.auth(token) } // AFE, ISE by check
                post = Post(id) // ISE by check
                user.checkAccess(post) // NAE
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            } catch (e: NotAccessibleException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@get
            }
            if(user.isParent && user.revealTime != null) {
                if (revealDateTime(user)!! > post.createdAt) {
                    call.response.status(HttpStatusCode.Forbidden)
                    return@get
                }
            }
            post.like(user)
            call.response.status(HttpStatusCode.OK)
            call.respond(post)
        }
        post("/create") {
            val multipart = call.receiveMultipart()

            var id: Int? = null
            var receiver: Int? = null
            var token: String? = null
            var title: String? = null
            var content: String? = null
            var ext: String? = null

            val post: Post

            val user: User

            val path: String = "/app/mogj"
            var streamProvider: (() -> InputStream)? = null

            try {
                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            ext = File(part.originalFileName!!).extension
                            streamProvider = part.streamProvider
                        }
                        is PartData.FormItem -> {
                            if (part.name == "id") id = part.value.toInt()
                            if (part.name == "token") token = part.value
                            if (part.name == "receiver") receiver = part.value.toInt()
                            if (part.name == "title") title = part.value
                            if (part.name == "content") content = part.value
                        }
                    }
                }
                user = User(id!!)
                user.auth(token!!)
                user.checkAccess(receiver!!)

                post = Post.write(id!!, receiver!!, title!!, content!!)
                post.update(Posts.image_extension, ext)

                if (streamProvider != null) {
                    streamProvider!!().use { its ->
                        File("$path/posts/${post.id}.$ext").outputStream().buffered().use {
                            its.copyTo(it)
                        }
                    }
                }
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
            } catch (e: NotAccessibleException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@post
            }
            user.update(Users.point, user.point + if (ext != null) { 100 } else { 50 })
            call.response.status(HttpStatusCode.OK)
            call.respond(mapOf("postId" to post.id))
        }
        get("/{id}/comments") {
            val params = call.request.queryParameters

            val userId: Int
            val token: String
            val postId: Int
            val from: Long

            val user: User
            val post: Post

            try {
                userId = params["userId"]!!.toInt() // NPE, NFE
                token = params["token"]!! // NPE
                postId = call.parameters["id"]!!.toInt() // NFE
                from = params["from"]?.toLongOrNull() ?: 0L

                user = User(userId) // ISE by check
                user.auth(token) // AFE
                post = Post(postId) // ISE by check
                user.checkAccess(post) // NAE
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@get
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@get
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            } catch (e: NotAccessibleException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@get
            }
            call.response.status(HttpStatusCode.OK)
            call.respond(post.comments(from))
        }
        delete("/{id}") {
            val params = call.receiveParameters()

            val userId: Int
            val token: String
            val id: Int

            val user: User
            val post: Post
            try {
                userId = params["userId"]!!.toInt() // NFE
                token = params["token"]!! // NPE
                id = call.parameters["id"]!!.toInt() // NPE, NFE

                user = User(userId) // ISE by check
                post = Post(id) // ISE by check
                user.auth(token) // AFE
            } catch (e: NumberFormatException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@delete
            } catch (e: NullPointerException) {
                call.response.status(HttpStatusCode.BadRequest)
                return@delete
            } catch (e: AuthFailException) {
                call.response.status(HttpStatusCode.Forbidden)
                return@delete
            } catch (e: IllegalStateException) {
                call.response.status(HttpStatusCode.NotFound)
                return@delete
            }
            if(user.id != post.writer) {
                call.response.status(HttpStatusCode.NotFound)
                return@delete
            }

            post.update(Posts.title, "Deleted Post $id")
            call.response.status(HttpStatusCode.OK)
        }
    }

}