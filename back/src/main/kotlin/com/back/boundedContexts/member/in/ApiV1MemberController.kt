package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import com.back.global.web.util.Rq
import com.back.standard.extensions.getOrThrow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/member/api/v1/members")
@Tag(name = "ApiV1ActorController", description = "API 회원 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1MemberController(
    private val memberFacade: MemberFacade,
    private val rq: Rq
) {
    @GetMapping("/randomSecureTip")
    @Operation(summary = "랜덤하게 보안 팁 반환")
    fun randomSecureTip() = "비밀번호는 영문, 숫자, 특수문자를 조합하여 8자 이상으로 설정하세요."

    @GetMapping("/{id}/redirectToProfileImg")
    @ResponseStatus(HttpStatus.FOUND)
    @Transactional(readOnly = true)
    @Operation(summary = "프로필 이미지 리다이렉트(브라우저 캐시 20분)")
    fun redirectToProfileImg(@PathVariable id: Int): ResponseEntity<Void> {
        val member = memberFacade.findById(id).getOrThrow()

        val cacheControl = CacheControl
            .maxAge(20, TimeUnit.MINUTES)
            .cachePublic()
            .immutable()

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(member.profileImgUrlOrDefault))
            .cacheControl(cacheControl)
            .build()
    }


    data class MemberJoinReqBody(
        @field:NotBlank @field:Size(min = 2, max = 30)
        val username: String,
        @field:NotBlank @field:Size(min = 2, max = 30)
        val password: String,
        @field:NotBlank @field:Size(min = 2, max = 30)
        val nickname: String,
    )

    @PostMapping
    @Transactional
    @Operation(summary = "가입")
    fun join(
        @RequestBody @Valid reqBody: MemberJoinReqBody
    ): RsData<MemberDto> {
        val member = memberFacade.join(
            reqBody.username,
            reqBody.password,
            reqBody.nickname
        )

        return RsData(
            "201-1",
            "${member.name}님 환영합니다. 회원가입이 완료되었습니다.",
            MemberDto(member)
        )
    }


    data class MemberLoginReqBody(
        @field:NotBlank @field:Size(min = 2, max = 30)
        val username: String,
        @field:NotBlank @field:Size(min = 2, max = 30)
        val password: String,
    )

    data class MemberLoginResBody(
        val item: MemberDto,
        val apiKey: String,
        val accessToken: String
    )

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "로그인")
    fun login(
        @RequestBody @Valid reqBody: MemberLoginReqBody
    ): RsData<MemberLoginResBody> {
        val member = memberFacade
            .findByUsername(reqBody.username)
            ?: throw BusinessException("401-1", "존재하지 않는 아이디입니다.")

        memberFacade.checkPassword(
            member,
            reqBody.password
        )

        val accessToken = memberFacade.genAccessToken(member)

        rq.setCookie("apiKey", member.apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200-1",
            "${member.name}님 환영합니다.",
            MemberLoginResBody(
                MemberDto(member),
                member.apiKey,
                accessToken
            )
        )
    }


    @DeleteMapping("/logout")
    @Operation(summary = "로그아웃")
    fun logout(): RsData<Void> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData(
            "200-1",
            "로그아웃 되었습니다."
        )
    }


    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "내 정보")
    fun me(): MemberWithUsernameDto {
        val actor = rq.actor

        return MemberWithUsernameDto(actor)
    }
}