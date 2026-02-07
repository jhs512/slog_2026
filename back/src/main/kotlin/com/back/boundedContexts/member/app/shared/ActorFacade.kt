package com.back.boundedContexts.member.app.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.out.shared.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class ActorFacade(
    private val authTokenService: AuthTokenService,
    private val memberRepository: MemberRepository,
) {
    @Transactional(readOnly = true)
    fun findByUsername(username: String): Member? = memberRepository.findByUsername(username)

    @Transactional(readOnly = true)
    fun findByApiKey(apiKey: String): Member? = memberRepository.findByApiKey(apiKey)

    fun genAccessToken(member: Member): String = authTokenService.genAccessToken(member)

    fun payload(accessToken: String) = authTokenService.payload(accessToken)

    @Transactional(readOnly = true)
    fun findById(id: Int): Member? = memberRepository.findById(id).getOrNull()

    fun getReferenceById(id: Int): Member = memberRepository.getReferenceById(id)
}