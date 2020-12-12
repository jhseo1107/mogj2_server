package dev.jhseo.mogj.server.controller

import dev.jhseo.mogj.server.AuthFailException
import dev.jhseo.mogj.server.NotAccessibleException
import dev.jhseo.mogj.server.db.Comments
import dev.jhseo.mogj.server.db.Users
import dev.jhseo.mogj.server.model.Comment
import dev.jhseo.mogj.server.model.Post
import dev.jhseo.mogj.server.model.User
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.lang.IllegalStateException
import java.lang.NullPointerException
import java.lang.NumberFormatException

fun Route.commentRoutes() {
    route("/comments") {
        post("/create") {
            val params = call.receiveParameters()

            val postId: Int
            val content: String
            val userId: Int
            val token: String

            val user: User
            val post: Post

            try {
                postId = params["postId"]!!.toInt() // NPE, NFE
                content = params["content"]!! // NPE
                userId = params["userId"]!!.toInt() // NPE, NFE
                token = params["token"]!! // NPE

                user = User(userId) // ISE by check
                user.auth(token) // AFE
                post = Post(postId) // ISE by check
                user.checkAccess(post) // NAE
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
            Comment.write(userId, postId, content)
            user.update(Users.point, user.point + 30)
            call.response.status(HttpStatusCode.OK)
        }
        delete("/{id}") {
            val params = call.receiveParameters()

            val commentId: Int
            val userId: Int
            val token: String

            val user: User
            val comment: Comment

            try {
                commentId = call.parameters["id"]!!.toInt() // NPE, NFE
                userId = params["userId"]!!.toInt() // NPE, NFE
                token = params["token"]!! // NPE

                user = User(userId) // ISE by check
                user.auth(token) // AFE
                comment = Comment(commentId) // ISE by check
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
            if (comment.writer != userId) {
                call.response.status(HttpStatusCode.Forbidden)
                return@delete
            }
            comment.update(Comments.content, "Deleted Comment $commentId")
            call.response.status(HttpStatusCode.OK)
        }
    }
}