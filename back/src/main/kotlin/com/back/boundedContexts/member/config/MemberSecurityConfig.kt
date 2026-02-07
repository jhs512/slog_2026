package com.back.boundedContexts.member.config

import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl
import org.springframework.stereotype.Component

@Component
class MemberSecurityConfig {
    fun configure(authorize: AuthorizeHttpRequestsDsl) {
        authorize.apply {
            authorize("/member/api/*/members/login", permitAll)
            authorize("/member/api/*/members/logout", permitAll)
            authorize(HttpMethod.POST, "/member/api/*/members", permitAll)
            authorize(HttpMethod.GET, "/member/api/*/members/{id:\\d+}/redirectToProfileImg", permitAll)
        }
    }
}