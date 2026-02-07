package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.standard.dto.member.type1.MemberSearchKeywordType1
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MemberRepositoryCustom {
    fun findByUsername(username: String): Member?
    fun findQPagedByKw(kwType: MemberSearchKeywordType1, kw: String, pageable: Pageable): Page<Member>
}
