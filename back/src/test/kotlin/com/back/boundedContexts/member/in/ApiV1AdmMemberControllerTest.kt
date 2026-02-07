package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.standard.extensions.getOrThrow
import org.hamcrest.Matchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1AdmMemberControllerTest {
    @Autowired
    private lateinit var memberFacade: MemberFacade

    @Autowired
    private lateinit var mvc: MockMvc


    @Nested
    @DisplayName("GET /member/api/v1/adm/members — 회원 다건조회")
    inner class GetItems {
        @Test
        @DisplayName("성공: 관리자가 조회")
        @WithUserDetails("admin")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members?page=1&pageSize=5")
                )
                .andDo(print())

            val members = memberFacade.findPaged(1, 5).content

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItems"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(members.size))

            for (i in members.indices) {
                val member = members[i]
                resultActions
                    .andExpect(jsonPath("$.content[$i].id").value(member.id))
                    .andExpect(
                        jsonPath("$.content[$i].createdAt")
                            .value(Matchers.startsWith(member.createdAt.toString().take(20)))
                    )
                    .andExpect(
                        jsonPath("$.content[$i].modifiedAt")
                            .value(Matchers.startsWith(member.modifiedAt.toString().take(20)))
                    )
                    .andExpect(jsonPath("$.content[$i].name").value(member.name))
                    .andExpect(jsonPath("$.content[$i].username").value(member.username))
                    .andExpect(jsonPath("$.content[$i].isAdmin").value(member.isAdmin))
            }
        }

        @Test
        @DisplayName("실패: 일반 사용자가 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자`() {
            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }
    }


    @Nested
    @DisplayName("GET /member/api/v1/adm/members/{id} — 회원 단건조회")
    inner class GetItem {
        @Test
        @DisplayName("성공: 관리자가 조회")
        @WithUserDetails("admin")
        fun `성공`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members/$id")
                )
                .andDo(print())

            val member = memberFacade.findById(id).getOrThrow()

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmMemberController::class.java))
                .andExpect(handler().methodName("getItem"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(member.id))
                .andExpect(
                    jsonPath("$.createdAt")
                        .value(Matchers.startsWith(member.createdAt.toString().take(20)))
                )
                .andExpect(
                    jsonPath("$.modifiedAt")
                        .value(Matchers.startsWith(member.modifiedAt.toString().take(20)))
                )
                .andExpect(jsonPath("$.name").value(member.name))
                .andExpect(jsonPath("$.username").value(member.username))
                .andExpect(jsonPath("$.isAdmin").value(member.isAdmin))
        }

        @Test
        @DisplayName("실패: 일반 사용자가 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자`() {
            val id = 1

            val resultActions = mvc
                .perform(
                    get("/member/api/v1/adm/members/$id")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }
    }
}
