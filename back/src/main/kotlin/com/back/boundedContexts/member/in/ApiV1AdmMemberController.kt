package com.back.boundedContexts.member.`in`

import PageDto
import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.standard.dto.member.type1.MemberSearchKeywordType1
import com.back.standard.dto.member.type1.MemberSearchSortType1
import com.back.standard.extensions.getOrThrow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/member/api/v1/adm/members")
@Tag(name = "ApiV1AdmMemberController", description = "관리자용 API 회원 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1AdmMemberController(
    private val memberFacade: MemberFacade
) {
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getItems(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        @RequestParam(defaultValue = "ALL") kwType: MemberSearchKeywordType1,
        @RequestParam(defaultValue = "") kw: String,
        @RequestParam(defaultValue = "ID") sort: MemberSearchSortType1,
    ): PageDto<MemberWithUsernameDto> {
        val page: Int = if (page >= 1) {
            page
        } else {
            1
        }

        val pageSize: Int = if (pageSize in 1..30) {
            pageSize
        } else {
            5
        }

        val memberPage = memberFacade.findPagedByKw(kwType, kw, sort, page, pageSize)

        return PageDto(
            memberPage
                .map { member -> MemberWithUsernameDto(member) }
        )
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getItem(
        @PathVariable id: Int
    ): MemberWithUsernameDto {
        val member = memberFacade.findById(id).getOrThrow()

        return MemberWithUsernameDto(member)
    }
}