package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.postExtensions.findCommentById
import com.back.boundedContexts.post.domain.postExtensions.getComments
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1PostCommentControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var actorFacade: ActorFacade


    @Nested
    @DisplayName("GET /post/api/v1/posts/{postId}/comments — 댓글 다건조회")
    inner class GetItems {
        @Test
        @DisplayName("성공: 댓글 목록 조회")
        fun `성공`() {
            val postId = 1

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$postId/comments")
                )
                .andDo(print())

            val post = postFacade.findById(postId).getOrThrow()
            val comments = post.getComments()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(comments.size))

            for (i in comments.indices) {
                val postComment = comments[i]

                resultActions
                    .andExpect(jsonPath("$[$i].id").value(postComment.id))
                    .andExpect(
                        jsonPath("$[$i].createdAt").value(
                            Matchers.startsWith(
                                postComment.createdAt.toString().take(20)
                            )
                        )
                    )
                    .andExpect(
                        jsonPath("$[$i].modifiedAt").value(
                            Matchers.startsWith(
                                postComment.modifiedAt.toString().take(20)
                            )
                        )
                    )
                    .andExpect(jsonPath("$[$i].authorId").value(postComment.author.id))
                    .andExpect(jsonPath("$[$i].authorName").value(postComment.author.name))
                    .andExpect(jsonPath("$[$i].postId").value(postComment.post.id))
                    .andExpect(jsonPath("$[$i].content").value(postComment.content))
            }
        }

        @Test
        @DisplayName("실패: 존재하지 않는 글 → 404")
        fun `실패 - 존재하지 않는 글`() {
            val postId = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$postId/comments")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("GET /post/api/v1/posts/{postId}/comments/{id} — 댓글 단건조회")
    inner class GetItem {
        @Test
        @DisplayName("성공: 댓글 조회")
        fun `성공`() {
            val postId = 1
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$postId/comments/$id")
                )
                .andDo(print())

            val post = postFacade.findById(postId).getOrThrow()
            val postComment = post.findCommentById(id).getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(postComment.id))
                .andExpect(
                    jsonPath("$.createdAt").value(
                        Matchers.startsWith(
                            postComment.createdAt.toString().take(20)
                        )
                    )
                )
                .andExpect(
                    jsonPath("$.modifiedAt").value(
                        Matchers.startsWith(
                            postComment.modifiedAt.toString().take(20)
                        )
                    )
                )
                .andExpect(jsonPath("$.authorId").value(postComment.author.id))
                .andExpect(jsonPath("$.authorName").value(postComment.author.name))
                .andExpect(jsonPath("$.postId").value(postComment.post.id))
                .andExpect(jsonPath("$.content").value(postComment.content))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 댓글 → 404")
        fun `실패 - 존재하지 않는 댓글`() {
            val postId = 1
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$postId/comments/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("POST /post/api/v1/posts/{postId}/comments — 댓글 작성")
    inner class Write {
        @Test
        @DisplayName("성공: 댓글 작성")
        @WithUserDetails("user1")
        fun `성공`() {
            val postId = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$postId/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            val post = postFacade.findById(postId).getOrThrow()
            val postComment = post.getComments().first()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("${postComment.id}번 댓글이 작성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(postComment.id))
                .andExpect(
                    jsonPath("$.data.createdAt").value(
                        Matchers.startsWith(
                            postComment.createdAt.toString().take(20)
                        )
                    )
                )
                .andExpect(
                    jsonPath("$.data.modifiedAt").value(
                        Matchers.startsWith(
                            postComment.modifiedAt.toString().take(20)
                        )
                    )
                )
                .andExpect(jsonPath("$.data.authorId").value(postComment.author.id))
                .andExpect(jsonPath("$.data.authorName").value(postComment.author.name))
                .andExpect(jsonPath("$.data.postId").value(postComment.post.id))
                .andExpect(jsonPath("$.data.content").value("내용"))
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val postId = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$postId/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }

        @Test
        @DisplayName("실패: 빈 내용 → 400")
        @WithUserDetails("user1")
        fun `실패 - 빈 내용`() {
            val postId = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$postId/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "content": ""
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
        }
    }


    @Nested
    @DisplayName("PUT /post/api/v1/posts/{postId}/comments/{id} — 댓글 수정")
    inner class Modify {
        @Test
        @DisplayName("성공: 작성자가 수정")
        @WithUserDetails("user1")
        fun `성공`() {
            val postId = 1
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$postId/comments/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("${id}번 댓글이 수정되었습니다."))
        }

        @Test
        @DisplayName("실패: 권한 없는 사용자 → 403")
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val postId = 1
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$postId/comments/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("작성자만 댓글을 수정할 수 있습니다."))
        }
    }


    @Nested
    @DisplayName("DELETE /post/api/v1/posts/{postId}/comments/{id} — 댓글 삭제")
    inner class Delete {
        @Test
        @DisplayName("성공: 작성자가 삭제")
        @WithUserDetails("user1")
        fun `성공`() {
            val postId = 1
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$postId/comments/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("${id}번 댓글이 삭제되었습니다."))
        }

        @Test
        @DisplayName("실패: 권한 없는 사용자 → 403")
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val postId = 1
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$postId/comments/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostCommentController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-2"))
                .andExpect(jsonPath("$.msg").value("작성자만 댓글을 삭제할 수 있습니다."))
        }
    }
}
