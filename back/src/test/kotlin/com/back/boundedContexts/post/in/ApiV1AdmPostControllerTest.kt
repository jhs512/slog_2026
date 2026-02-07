package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.post.app.PostFacade
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
class ApiV1AdmPostControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var postFacade: PostFacade


    @Nested
    @DisplayName("GET /post/api/v1/adm/posts/count — 글 개수 조회")
    inner class Count {
        @Test
        @DisplayName("성공: 관리자가 조회")
        @WithUserDetails("admin")
        fun `성공`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/adm/posts/count")
                )
                .andDo(print())

            resultActions
                .andExpect(handler().handlerType(ApiV1AdmPostController::class.java))
                .andExpect(handler().methodName("count"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.all").value(postFacade.count()))
                .andExpect(jsonPath("$.secureTip").isNotEmpty())
        }

        @Test
        @DisplayName("실패: 일반 사용자가 조회 → 403")
        @WithUserDetails("user1")
        fun `실패 - 일반 사용자`() {
            val resultActions = mvc
                .perform(
                    get("/post/api/v1/adm/posts/count")
                )
                .andDo(print())

            resultActions
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.resultCode").value("403-1"))
                .andExpect(jsonPath("$.msg").value("권한이 없습니다."))
        }
    }
}
