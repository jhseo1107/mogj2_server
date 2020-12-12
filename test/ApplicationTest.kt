package dev.jhseo

import dev.jhseo.hasher.HasherMethod
import dev.jhseo.hasher.hash
import dev.jhseo.mogj.server.module
import io.ktor.http.*
import io.ktor.http.content.*
import kotlin.test.*
import io.ktor.server.testing.*
import io.ktor.utils.io.streams.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.io.FileInputStream

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            val jsonParser = JSONParser()

            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }

            handleRequest(HttpMethod.Post, "/users/create") {
                val kakaoId = "1"
                val nickname = "testChild"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }

            handleRequest(HttpMethod.Post, "/users/create") { // User kakaoId same
                val kakaoId = "1"
                val nickname = "testChild"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }. apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }

            val token: String

            handleRequest(HttpMethod.Get, "/users/1/token?password=wrong").apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
            handleRequest(HttpMethod.Get, "/users/wronguri/token?password=asdfasdf").apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
            }
            handleRequest(HttpMethod.Get, "/users/999999/token?password=asdfasdf").apply { // Null User
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/users/1/token?password=asdfasdf").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                token = (jsonParser.parse(response.content) as JSONObject)["token"] as String
            }

            handleRequest(HttpMethod.Get, "/users/1/is_valid?token=${token}3").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(false, (jsonParser.parse(response.content) as JSONObject)["valid"] as Boolean)
            }
            handleRequest(HttpMethod.Get, "/users/1/is_valid?token=$token").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(true, (jsonParser.parse(response.content) as JSONObject)["valid"] as Boolean)
            }

            handleRequest(HttpMethod.Get, "/users/1").apply {
                println("${response.content}")
            }

            handleRequest(HttpMethod.Patch, "/users/1") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("nickname" to "testParent1", "token" to token).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            val tokens: MutableList<String> = mutableListOf("", "", "", "", "", "")
            tokens[1] = token

            handleRequest(HttpMethod.Get, "/users/1").apply {
                println("${response.content}")
            }

            handleRequest(HttpMethod.Post, "/users/create") {
                val kakaoId = "2"
                val nickname = "testParent2"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }

            handleRequest(HttpMethod.Post, "/users/create") {
                val kakaoId = "3"
                val nickname = "testChild1"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }

            handleRequest(HttpMethod.Post, "/users/create") {
                val kakaoId = "4"
                val nickname = "testChild2"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }

            handleRequest(HttpMethod.Post, "/users/create") {
                val kakaoId = "5"
                val nickname = "testChild3"
                val avatar = "https://avatars3.githubusercontent.com/u/33977729?s=460&v=4"
                val password = "asdfasdf"
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("kakaoId" to kakaoId, "nickname" to nickname, "avatar" to avatar, "password" to password).formUrlEncode())
            }

            (2..5).forEach {
                handleRequest(HttpMethod.Get, "/users/$it/token?password=asdfasdf").apply {
                    tokens[it] = (jsonParser.parse(response.content) as JSONObject)["token"] as String
                }
            }

            (1..2).forEach {
                handleRequest(HttpMethod.Patch, "/users/$it") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("isParent" to "true", "token" to tokens[it]).formUrlEncode())
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }

            (3..5).forEach {
                handleRequest(HttpMethod.Patch, "/users/$it") {
                    addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    setBody(listOf("isChild" to "true", "token" to tokens[it]).formUrlEncode())
                }.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                }
            }

            // Child1 and Child2 in same house with parent1, Child3 has both parents

            // Child1 creates house
            handleRequest(HttpMethod.Post, "/houses/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "3", "token" to tokens[3]).formUrlEncode())
            }
            // Child1 generates invitation to child2.
            var invite: String
            handleRequest(HttpMethod.Post, "/invites/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "3", "token" to tokens[3]).formUrlEncode())
            }.apply {
                invite = (jsonParser.parse(response.content) as JSONObject)["invite"] as String
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Child2 uses invitation.
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "4", "token" to tokens[4]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Now Child2 should be in house 1
            handleRequest(HttpMethod.Get, "/users/4").apply {
                assertEquals(1L, (jsonParser.parse(response.content) as JSONObject)["house"])
            }
            // Child3 uses expired invitation -> fail
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "5", "token" to tokens[5]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.Conflict, response.status())
            }
            // Child2 generates invitation for Parent1
            handleRequest(HttpMethod.Post, "/invites/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "4", "token" to tokens[4]).formUrlEncode())
            }.apply {
                invite = (jsonParser.parse(response.content) as JSONObject)["invite"] as String
            }
            // Parent1 uses invitation.
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "1", "token" to tokens[1]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Now Parent1 should be in house 1
            handleRequest(HttpMethod.Get, "/houses/1").apply {
                assert(1L in (jsonParser.parse(response.content) as JSONObject)["parents"] as List<*>)
            }
            // Child3 Make House
            handleRequest(HttpMethod.Post, "/houses/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "5", "token" to tokens[5]).formUrlEncode())
            }
            // Child3 Generates invite 1
            handleRequest(HttpMethod.Post, "/invites/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "5", "token" to tokens[5]).formUrlEncode())
            }.apply {
                invite = (jsonParser.parse(response.content) as JSONObject)["invite"] as String
            }
            // Parent1 uses invitation.
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "1", "token" to tokens[1]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Child3 Generates invite 2
            handleRequest(HttpMethod.Post, "/invites/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "5", "token" to tokens[5]).formUrlEncode())
            }.apply {
                invite = (jsonParser.parse(response.content) as JSONObject)["invite"] as String
            }
            // Parent2 uses invitation.
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "2", "token" to tokens[2]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Run ParentMatcher
            handleRequest(HttpMethod.Get, "/runParentMatcher").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Write Post from Child1 -> Parent2 : Forbidden
            handleRequest(HttpMethod.Post, "/posts/create") {
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", "***bbb***").toString())
                setBody("***bbb***", listOf(
                    PartData.FormItem("3", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "id").toString()
                    )),
                    PartData.FormItem(tokens[3], { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "token").toString()
                    )),
                    PartData.FormItem("2", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "receiver").toString()
                    )),
                    PartData.FormItem("sdf", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "title").toString()
                    )),
                    PartData.FormItem("asdf", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "content").toString()
                    ))
                ))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
            // Write Post from Child1 -> Parent1 : Success
            handleRequest(HttpMethod.Post, "/posts/create") {
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", "***bbb***").toString())
                setBody("***bbb***", listOf(
                    PartData.FormItem("3", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "id").toString()
                    )),
                    PartData.FormItem(tokens[3], { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "token").toString()
                    )),
                    PartData.FormItem("1", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "receiver").toString()
                    )),
                    PartData.FormItem("sdf", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "title").toString()
                    )),
                    PartData.FormItem("asdf", { }, headersOf(
                        HttpHeaders.ContentDisposition, ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "content").toString()
                    ))
                ))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Write Post from Child3 -> Parent2 With Image: Sucess
            handleRequest(HttpMethod.Post, "/posts/create") {
                addHeader(HttpHeaders.ContentType, ContentType.MultiPart.FormData.withParameter("boundary", "***bbb***").toString())
                setBody(
                    "***bbb***", listOf(
                        PartData.FormItem(
                            "5", { }, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "id")
                                    .toString()
                            )
                        ),
                        PartData.FormItem(
                            tokens[5], { }, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "token")
                                    .toString()
                            )
                        ),
                        PartData.FormItem(
                            "2", { }, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "receiver")
                                    .toString()
                            )
                        ),
                        PartData.FormItem(
                            "sdf", { }, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "title")
                                    .toString()
                            )
                        ),
                        PartData.FormItem(
                            "asdf", { }, headersOf(
                                HttpHeaders.ContentDisposition,
                                ContentDisposition.Inline.withParameter(ContentDisposition.Parameters.Name, "content")
                                    .toString()
                            )
                        ),
                        PartData.FileItem({ FileInputStream(File("${System.getenv("PROJECT_PATH")}/test/testImage.png")).asInput() }, { }, headersOf(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.File
                                .withParameter(ContentDisposition.Parameters.Name, "testImage")
                                .withParameter(ContentDisposition.Parameters.FileName, "testImage.png")
                                .toString()
                        ))
                    )
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Get Posts as Child1 -> one post
            handleRequest(HttpMethod.Get, "/posts?id=3&token=${tokens[3]}").apply {
                assertEquals(1, (jsonParser.parse(response.content) as JSONArray).size)
                assertEquals("sdf", ((jsonParser.parse(response.content) as JSONArray)[0] as JSONObject)["title"] as String)
            }
            // Get Posts as Child3 -> two posts
            handleRequest(HttpMethod.Get, "/posts?id=5&token=${tokens[5]}").apply {
                assertEquals(2, (jsonParser.parse(response.content) as JSONArray).size)
            }
            // Get Posts as Child3 from 1 -> one post
            handleRequest(HttpMethod.Get, "/posts?id=5&token=${tokens[5]}&from=1").apply {
                assertEquals(1, (jsonParser.parse(response.content) as JSONArray).size)
            }
            // Get Posts as Parent1 -> two posts : #1 as receiver 1, #2 as writer 5
            handleRequest(HttpMethod.Get, "/posts?id=1&token=${tokens[1]}").apply {
                assertEquals(2, (jsonParser.parse(response.content) as JSONArray).size)
            }
            // Child1 can't access to post 2 -> comment fail
            handleRequest(HttpMethod.Post, "/comments/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "3", "token" to tokens[3], "postId" to "2", "content" to "asdf").formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, response.status())
            }
            // Child1 can access to post 1 -> comment success
            handleRequest(HttpMethod.Post, "/comments/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "3", "token" to tokens[3], "postId" to "1", "content" to "asdf").formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Parent1 can access to post 1 -> comment success
            handleRequest(HttpMethod.Post, "/comments/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "1", "token" to tokens[1], "postId" to "1", "content" to "asdf").formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Child1 point -> 80
            handleRequest(HttpMethod.Get, "/users/3").apply {
                assertEquals(80L, (jsonParser.parse(response.content) as JSONObject)["point"] as Long)
            }
            // Child3 point should be 100
            handleRequest(HttpMethod.Get, "/users/5").apply {
                assertEquals(100L, (jsonParser.parse(response.content) as JSONObject)["point"] as Long)
            }
            // Delete comment 2
            handleRequest(HttpMethod.Delete, "/comments/2") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "1", "token" to tokens[1]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // View post 1's second comment -> Deleted comment 2
            handleRequest(HttpMethod.Get, "/posts/1/comments?userId=3&token=${tokens[3]}").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Deleted Comment 2", ((jsonParser.parse(response.content) as JSONArray)[0] as JSONObject)["content"])
            }
            // Delete post 2
            handleRequest(HttpMethod.Delete, "/posts/2") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "5", "token" to tokens[5]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // View post 1 -> Deleted post 2
            handleRequest(HttpMethod.Get, "/posts/2?userId=5&token=${tokens[5]}").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Deleted Post 2", (jsonParser.parse(response.content) as JSONObject)["title"])
            }
            // Change ParentMatcher Value
            // Child1 Generates invite 3
            handleRequest(HttpMethod.Post, "/invites/create") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("id" to "3", "token" to tokens[3]).formUrlEncode())
            }.apply {
                invite = (jsonParser.parse(response.content) as JSONObject)["invite"] as String
            }
            // Parent2 uses invite 3
            handleRequest(HttpMethod.Post, invite) {
                addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                setBody(listOf("userId" to "2", "token" to tokens[2]).formUrlEncode())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Run ParentMatcher
            handleRequest(HttpMethod.Get, "/runParentMatcher").apply {
                assertEquals(HttpStatusCode.OK, response.status())
            }
            // Now it should be parent1 - house2, parent2 - house1
        }
    }


}
