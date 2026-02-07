package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.postExtensions.likesCount
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
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
class ApiV1PostControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var actorFacade: ActorFacade


    @Nested
    @DisplayName("POST /post/api/v1/posts — 글 작성")
    inner class Write {
        @Test
        @DisplayName("성공: 기본 작성 (published=false)")
        @WithUserDetails("user1")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            val post = postFacade.findLatest().getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 작성되었습니다.".format(post.id)))
                .andExpect(jsonPath("$.data.id").value(post.id))
                .andExpect(jsonPath("$.data.createdAt").value(Matchers.startsWith(post.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.data.modifiedAt").value(Matchers.startsWith(post.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.authorId").value(post.author.id))
                .andExpect(jsonPath("$.data.authorName").value(post.author.name))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.listed").value(false))
        }

        @Test
        @DisplayName("성공: published=true, listed=true로 작성")
        @WithUserDetails("user1")
        fun `성공 - 공개 글 작성`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "공개 글",
                                "content": "내용",
                                "published": true,
                                "listed": true
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.published").value(true))
                .andExpect(jsonPath("$.data.listed").value(true))
        }

        @Test
        @DisplayName("성공: wrong apiKey + valid accessToken → accessToken 우선")
        fun `성공 - wrong apiKey with valid accessToken`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorAccessToken = actorFacade.genAccessToken(actor)

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-api-key $actorAccessToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
        }

        @Test
        @DisplayName("성공: wrong apiKey cookie + valid accessToken cookie")
        fun `성공 - wrong apiKey cookie with valid accessToken cookie`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorAccessToken = actorFacade.genAccessToken(actor)

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .cookie(
                            Cookie("apiKey", "wrong-api-key"),
                            Cookie("accessToken", actorAccessToken)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isCreated)
        }

        @Test
        @DisplayName("실패: 제목 없이 → 400")
        @WithUserDetails("user1")
        fun `실패 - 제목 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value(
                        """
                        title-NotBlank-must not be blank
                        title-Size-size must be between 2 and 100
                        """.trimIndent()
                    )
                )
        }

        @Test
        @DisplayName("실패: 내용 없이 → 400")
        @WithUserDetails("user1")
        fun `실패 - 내용 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": ""
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value(
                        """
                        content-NotBlank-must not be blank
                        content-Size-size must be between 2 and 2147483647
                        """.trimIndent()
                    )
                )
        }

        @Test
        @DisplayName("실패: 잘못된 JSON → 400")
        @WithUserDetails("user1")
        fun `실패 - 잘못된 JSON`() {
            val wrongJsonBody = """
                {
                    "title": 제목",
                    content": "내용"

                """.trimIndent()

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongJsonBody)
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("write"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(
                    jsonPath("$.msg").value("요청 본문이 올바르지 않습니다.".trim())
                )
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목",
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
        @DisplayName("실패: 잘못된 인증 헤더 → 401")
        fun `실패 - 잘못된 인증 헤더`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer wrong-api-key")
                        .content(
                            """
                            {
                                "title": "제목",
                                "content": "내용"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-3"))
                .andExpect(jsonPath("$.msg").value("API 키가 유효하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("GET /post/api/v1/posts/{id} — 글 단건조회")
    inner class GetItem {
        @Test
        @DisplayName("성공: 공개 글 조회")
        fun `성공 - 공개 글`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$id")
                )
                .andDo(print())

            val post = postFacade.findById(id).getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(post.id))
                .andExpect(
                    jsonPath("$.createdAt")
                        .value(Matchers.startsWith(post.createdAt.toString().take(20)))
                )
                .andExpect(
                    jsonPath("$.modifiedAt")
                        .value(Matchers.startsWith(post.modifiedAt.toString().take(20)))
                )
                .andExpect(jsonPath("$.authorId").value(post.author.id))
                .andExpect(jsonPath("$.authorName").value(post.author.name))
                .andExpect(jsonPath("$.title").value(post.title))
                .andExpect(jsonPath("$.content").value(post.content))
                .andExpect(jsonPath("$.published").value(post.published))
                .andExpect(jsonPath("$.listed").value(post.listed))
        }

        @Test
        @DisplayName("성공: 미공개 글 - 작성자 조회")
        @WithUserDetails("user1")
        fun `성공 - 미공개 글 작성자 조회`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val post = postFacade.write(actor, "미공개 글", "내용", published = false, listed = false)

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/${post.id}")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.published").value(false))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 글 → 404")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }

        @Test
        @DisplayName("실패: 미공개 글 - 다른 사용자 → 403")
        @WithUserDetails("user3")
        fun `실패 - 미공개 글 다른 사용자`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val post = postFacade.write(actor, "미공개 글", "내용", published = false, listed = false)

            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/${post.id}")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-3"))
        }
    }


    @Nested
    @DisplayName("GET /post/api/v1/posts — 글 다건조회")
    inner class GetItems {
        @Test
        @DisplayName("성공: 기본 페이징 조회")
        fun `성공 - 기본 페이징`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts?page=1&pageSize=5")
                )
                .andDo(print())

            val postPage = postFacade.findPagedByKw(
                PostSearchKeywordType1.ALL,
                "",
                PostSearchSortType1.ID,
                1,
                5
            )

            val posts = postPage.content

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(posts.size))

            for (i in posts.indices) {
                val post = posts[i]
                resultActions
                    .andExpect(jsonPath("$.content[%d].id".format(i)).value(post.id))
                    .andExpect(
                        jsonPath("$.content[%d].createdAt".format(i))
                            .value(Matchers.startsWith(post.createdAt.toString().take(20)))
                    )
                    .andExpect(
                        jsonPath("$.content[%d].modifiedAt".format(i))
                            .value(Matchers.startsWith(post.modifiedAt.toString().take(20)))
                    )
                    .andExpect(jsonPath("$.content[%d].authorId".format(i)).value(post.author.id))
                    .andExpect(jsonPath("$.content[%d].authorName".format(i)).value(post.author.name))
                    .andExpect(jsonPath("$.content[%d].title".format(i)).value(post.title))
            }
        }

        @Test
        @DisplayName("성공: 검색 키워드로 조회")
        fun `성공 - 키워드 검색`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts?page=1&pageSize=5&kwType=TITLE&kw=제목 1")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
        }
    }


    @Nested
    @DisplayName("PUT /post/api/v1/posts/{id} — 글 수정")
    inner class Modify {
        @Test
        @DisplayName("성공: 작성자가 수정")
        @WithUserDetails("user1")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 수정되었습니다.".format(id)))
        }

        @Test
        @DisplayName("실패: 권한 없는 사용자 → 403")
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 수정할 수 있습니다."))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 글 → 404")
        @WithUserDetails("user1")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    put("/post/api/v1/posts/$id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "title": "제목 new",
                                "content": "내용 new"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("modify"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("DELETE /post/api/v1/posts/{id} — 글 삭제")
    inner class Delete {
        @Test
        @DisplayName("성공: 작성자가 삭제")
        @WithUserDetails("user1")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".format(id)))
        }

        @Test
        @DisplayName("성공: 관리자가 삭제")
        @WithUserDetails("admin")
        fun `성공 - 관리자`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글이 삭제되었습니다.".format(id)))
        }

        @Test
        @DisplayName("실패: 권한 없는 사용자 → 403")
        @WithUserDetails("user3")
        fun `실패 - 권한 없음`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-2"))
                .andExpect(jsonPath("$.msg").value("작성자만 글을 삭제할 수 있습니다."))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 글 → 404")
        @WithUserDetails("user1")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    delete("/post/api/v1/posts/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("delete"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("POST /post/api/v1/posts/{id}/hit — 조회수 증가")
    inner class IncrementHit {
        @Test
        @DisplayName("성공: 조회수 증가")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/hit")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("incrementHit"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("조회수가 증가했습니다."))
                .andExpect(jsonPath("$.data.hitCount").isNumber)
        }

        @Test
        @DisplayName("실패: 존재하지 않는 글 → 404")
        fun `실패 - 존재하지 않는 글`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/hit")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("incrementHit"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("POST /post/api/v1/posts/{id}/like — 좋아요 토글")
    inner class ToggleLike {
        @Test
        @DisplayName("성공: 좋아요 추가")
        @WithUserDetails("user1")
        fun `성공 - 좋아요 추가`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("toggleLike"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좋아요를 눌렀습니다."))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCount").isNumber)
        }

        @Test
        @DisplayName("성공: 좋아요 취소 (두 번 토글)")
        @WithUserDetails("user1")
        fun `성공 - 좋아요 취소`() {
            val id = 1

            // 첫 번째: 좋아요 추가
            mvc.perform(post("/post/api/v1/posts/$id/like"))

            // 두 번째: 좋아요 취소
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("toggleLike"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("좋아요를 취소했습니다."))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCount").isNumber)
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/$id/like")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }


    @Nested
    @DisplayName("GET /post/api/v1/posts/mine — 내 게시물 조회")
    inner class GetMine {
        @Test
        @DisplayName("성공: 내 게시물 목록")
        @WithUserDetails("user1")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/mine?page=1&pageSize=10")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getMine"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/posts/mine?page=1&pageSize=10")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }


    @Nested
    @DisplayName("POST /post/api/v1/posts/temp — 임시저장")
    inner class GetOrCreateTemp {
        @Test
        @DisplayName("성공: 새 임시글 생성")
        @WithUserDetails("user1")
        fun `성공 - 새 임시글`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getOrCreateTemp"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.data.published").value(false))
                .andExpect(jsonPath("$.data.listed").value(false))
        }

        @Test
        @DisplayName("성공: 기존 임시글 반환")
        @WithUserDetails("user1")
        fun `성공 - 기존 임시글`() {
            // 첫 번째 호출: 임시글 생성
            mvc.perform(post("/post/api/v1/posts/temp"))

            // 두 번째 호출: 기존 임시글 반환
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1PostController::class.java))
                .andExpect(handler().methodName("getOrCreateTemp"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("기존 임시저장 글을 반환합니다."))
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    post("/post/api/v1/posts/temp")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }
}
