package com.back.global.security.config

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.app.app.AppFacade
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import com.back.global.security.domain.SecurityUser
import com.back.global.web.util.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val actorFacade: ActorFacade,
    private val rq: Rq,
) : OncePerRequestFilter() {

    private val publicApiPaths = setOf(
        "/member/api/v1/members/login",
        "/member/api/v1/members/logout",
        "/member/api/v1/members/join",
    )

    private val publicApiPatterns = listOf(
        Regex("/member/api/v1/members/\\d+/redirectToProfileImg")
    )

    private val apiPrefixes = listOf("/member/api/", "/post/api/")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        if (apiPrefixes.none { uri.startsWith(it) }) return true
        if (uri in publicApiPaths) return true
        if (publicApiPatterns.any { it.matches(uri) }) return true
        return false
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            work(request, response, filterChain)
        } catch (e: BusinessException) {
            val rsData: RsData<Void> = e.rsData
            response.contentType = "$APPLICATION_JSON_VALUE; charset=UTF-8"
            response.status = rsData.statusCode
            response.writer.write(Ut.JSON.toString(rsData))
        }
    }

    private fun work(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val (apiKey, accessToken) = extractTokens()
        if (apiKey.isBlank() && accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        if (apiKey == AppFacade.systemMemberApiKey && accessToken.isEmpty()) {
            authenticate(Member(1, "system", "시스템"))
            filterChain.doFilter(request, response)
            return
        }

        val (member, isAccessTokenValid) = resolveMember(apiKey, accessToken)

        if (accessToken.isNotBlank() && !isAccessTokenValid) {
            refreshAccessToken(member)
        }

        authenticate(member)

        filterChain.doFilter(request, response)
    }

    private fun extractTokens(): Pair<String, String> {
        val headerAuthorization = rq.getHeader(HttpHeaders.AUTHORIZATION, "")

        return if (headerAuthorization.isNotBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw BusinessException("401-2", "${HttpHeaders.AUTHORIZATION} 헤더가 Bearer 형식이 아닙니다.")
            val bits = headerAuthorization.split(" ", limit = 3)
            bits.getOrNull(1).orEmpty() to bits.getOrNull(2).orEmpty()
        } else {
            rq.getCookieValue("apiKey", "") to rq.getCookieValue("accessToken", "")
        }
    }

    private fun resolveMember(apiKey: String, accessToken: String): Pair<Member, Boolean> {
        memberFromAccessToken(accessToken)?.let { return it to true }

        val member = actorFacade.findByApiKey(apiKey)
            ?: throw BusinessException("401-3", "API 키가 유효하지 않습니다.")

        return member to false
    }

    private fun memberFromAccessToken(token: String): Member? {
        if (token.isBlank()) return null

        val payload = actorFacade.payload(token) ?: return null

        return Member(payload.id, payload.username, payload.name)
    }

    private fun refreshAccessToken(member: Member) {
        val newToken = actorFacade.genAccessToken(member)

        rq.setCookie("accessToken", newToken)
        rq.setHeader(HttpHeaders.AUTHORIZATION, newToken)
    }

    private fun authenticate(member: Member) {
        val user: UserDetails = SecurityUser(
            member.id,
            member.username,
            "",
            member.name,
            member.authorities
        )

        val authentication: Authentication =
            UsernamePasswordAuthenticationToken(user, user.password, user.authorities)

        SecurityContextHolder.getContext().authentication = authentication
    }
}