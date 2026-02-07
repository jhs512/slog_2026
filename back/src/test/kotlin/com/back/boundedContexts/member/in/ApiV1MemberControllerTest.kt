package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.standard.extensions.getOrThrow
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
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
class ApiV1MemberControllerTest {
    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var mvc: MockMvc


    @Nested
    @DisplayName("POST /member/api/v1/members — 회원가입")
    inner class Join {
        @Test
        @DisplayName("성공: 회원가입")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "usernew",
                                "password": "1234",
                                "nickname": "무명"
                            }
                            """
                        )
                )
                .andDo(print())

            val member = actorFacade.findByUsername("usernew").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("${member.name}님 환영합니다. 회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(member.id))
                .andExpect(jsonPath("$.data.createdAt").value(Matchers.startsWith(member.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.data.modifiedAt").value(Matchers.startsWith(member.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.data.name").value(member.name))
                .andExpect(jsonPath("$.data.isAdmin").value(member.isAdmin))
        }

        @Test
        @DisplayName("실패: 중복 username → 409")
        fun `실패 - 중복 username`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "1234",
                                "nickname": "중복유저"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 존재하는 회원 아이디입니다."))
        }

        @Test
        @DisplayName("실패: 유효성 검증 실패 → 400")
        fun `실패 - 유효성 검증 실패`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "",
                                "password": "",
                                "nickname": ""
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("join"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.resultCode").value("400-1"))
        }
    }


    @Nested
    @DisplayName("POST /member/api/v1/members/login — 로그인")
    inner class Login {
        @Test
        @DisplayName("성공: 로그인 + 쿠키 검증")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "1234"
                            }
                            """
                        )
                )
                .andDo(print())

            val member = actorFacade.findByUsername("user1").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("${member.nickname}님 환영합니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.item").exists())
                .andExpect(jsonPath("$.data.item.id").value(member.id))
                .andExpect(
                    jsonPath("$.data.item.createdAt").value(
                        Matchers.startsWith(
                            member.createdAt.toString().take(20)
                        )
                    )
                )
                .andExpect(
                    jsonPath("$.data.item.modifiedAt").value(
                        Matchers.startsWith(
                            member.modifiedAt.toString().take(20)
                        )
                    )
                )
                .andExpect(jsonPath("$.data.item.name").value(member.name))
                .andExpect(jsonPath("$.data.item.isAdmin").value(member.isAdmin))
                .andExpect(jsonPath("$.data.apiKey").value(member.apiKey))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty)

            resultActions.andExpect { result ->
                val apiKeyCookie = result.response.getCookie("apiKey").getOrThrow()
                assertThat(apiKeyCookie.value).isEqualTo(member.apiKey)
                assertThat(apiKeyCookie.path).isEqualTo("/")
                assertThat(apiKeyCookie.isHttpOnly).isTrue

                val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                assertThat(accessTokenCookie.value).isNotBlank
                assertThat(accessTokenCookie.path).isEqualTo("/")
                assertThat(accessTokenCookie.isHttpOnly).isTrue
            }
        }

        @Test
        @DisplayName("실패: 잘못된 비밀번호 → 401")
        fun `실패 - 잘못된 비밀번호`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "user1",
                                "password": "wrong-password"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("비밀번호가 일치하지 않습니다."))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자 → 401")
        fun `실패 - 존재하지 않는 사용자`() {
            val resultActions = mvc
                .perform(
                    post("/member/api/v1/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                                "username": "nonexistent",
                                "password": "1234"
                            }
                            """
                        )
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("login"))
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 아이디입니다."))
        }
    }


    @Nested
    @DisplayName("DELETE /member/api/v1/members/logout — 로그아웃")
    inner class Logout {
        @Test
        @DisplayName("성공: 로그아웃 + 쿠키 초기화")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    delete("/member/api/v1/members/logout")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("logout"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("로그아웃 되었습니다."))
                .andExpect { result ->
                    val apiKeyCookie = result.response.getCookie("apiKey").getOrThrow()
                    assertThat(apiKeyCookie.value).isEmpty()
                    assertThat(apiKeyCookie.maxAge).isEqualTo(0)
                    assertThat(apiKeyCookie.path).isEqualTo("/")
                    assertThat(apiKeyCookie.isHttpOnly).isTrue

                    val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                    assertThat(accessTokenCookie.value).isEmpty()
                    assertThat(accessTokenCookie.maxAge).isEqualTo(0)
                    assertThat(accessTokenCookie.path).isEqualTo("/")
                    assertThat(accessTokenCookie.isHttpOnly).isTrue
                }
        }
    }


    @Nested
    @DisplayName("GET /member/api/v1/members/me — 내 정보 조회")
    inner class Me {
        @Test
        @DisplayName("성공: @WithUserDetails로 조회")
        @WithUserDetails("user1")
        fun `성공 - WithUserDetails`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                )
                .andDo(print())

            val member = actorFacade.findByUsername("user1").getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(member.id))
                .andExpect(jsonPath("$.createdAt").value(Matchers.startsWith(member.createdAt.toString().take(20))))
                .andExpect(jsonPath("$.modifiedAt").value(Matchers.startsWith(member.modifiedAt.toString().take(20))))
                .andExpect(jsonPath("$.name").value(member.name))
                .andExpect(jsonPath("$.username").value(member.username))
                .andExpect(jsonPath("$.isAdmin").value(member.isAdmin))
        }

        @Test
        @DisplayName("성공: apiKey 쿠키로 조회")
        fun `성공 - apiKey 쿠키`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorApiKey: String = actor.apiKey

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .cookie(Cookie("apiKey", actorApiKey))
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("성공: 만료된 accessToken → apiKey로 재발급")
        fun `성공 - accessToken 재발급`() {
            val actor = actorFacade.findByUsername("user1").getOrThrow()
            val actorApiKey: String = actor.apiKey

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $actorApiKey wrong-access-token")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("me"))
                .andExpect(status().isOk)

            resultActions.andExpect { result ->
                val accessTokenCookie = result.response.getCookie("accessToken").getOrThrow()
                assertThat(accessTokenCookie.value).isNotBlank
                assertThat(accessTokenCookie.path).isEqualTo("/")
                assertThat(accessTokenCookie.isHttpOnly).isTrue

                val headerAuthorization = result.response.getHeader(HttpHeaders.AUTHORIZATION)
                assertThat(headerAuthorization).isNotBlank

                assertThat(headerAuthorization).isEqualTo(accessTokenCookie.value)
            }
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }

        @Test
        @DisplayName("실패: Bearer 형식 아님 → 401")
        fun `실패 - Bearer 형식 아님`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "key")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-2"))
                .andExpect(jsonPath("$.msg").value("Authorization 헤더가 Bearer 형식이 아닙니다."))
        }
    }


    @Nested
    @DisplayName("GET /member/api/v1/members/{id}/redirectToProfileImg — 프로필 이미지 리다이렉트")
    inner class RedirectToProfileImg {
        @Test
        @DisplayName("성공: 리다이렉트 응답")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/$id/redirectToProfileImg")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("redirectToProfileImg"))
                .andExpect(status().isFound)
                .andExpect(header().exists("Location"))
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 → 404")
        fun `실패 - 존재하지 않는 회원`() {
            val id = Int.MAX_VALUE

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/$id/redirectToProfileImg")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("redirectToProfileImg"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."))
        }
    }


    @Nested
    @DisplayName("GET /member/api/v1/members/randomSecureTip — 랜덤 보안 팁")
    inner class RandomSecureTip {
        @Test
        @DisplayName("성공: 보안 팁 반환")
        @WithUserDetails("user1")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/randomSecureTip")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1MemberController::class.java))
                .andExpect(handler().methodName("randomSecureTip"))
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("실패: 인증 없이 → 401")
        fun `실패 - 인증 없이`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/members/randomSecureTip")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.resultCode").value("401-1"))
                .andExpect(jsonPath("$.msg").value("로그인 후 이용해주세요."))
        }
    }
}
