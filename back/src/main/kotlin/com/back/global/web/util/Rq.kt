package com.back.global.web.util

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.domain.shared.MemberProxy
import com.back.global.app.app.AppFacade
import com.back.global.security.domain.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class Rq(
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse,
    private val actorFacade: ActorFacade,
) {
    val actorOrNull: Member?
        get() = (
                SecurityContextHolder
                    .getContext()
                    ?.authentication
                    ?.principal as? SecurityUser
                )
            ?.let {
                MemberProxy(
                    actorFacade.getReferenceById(it.id),
                    it.id,
                    it.username,
                    it.nickname
                )
            }

    val actor: Member
        get() = actorOrNull ?: throw IllegalStateException("인증된 사용자가 없습니다.")

    fun getHeader(name: String, defaultValue: String): String =
        req.getHeader(name) ?: defaultValue

    fun setHeader(name: String, value: String) {
        resp.setHeader(name, value)
    }

    fun getCookieValue(name: String, defaultValue: String): String =
        req.cookies
            ?.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue

    private fun cookieDomain(): String {
        val domain = AppFacade.siteCookieDomain

        // localhost는 그대로, 그 외에는 앞에 . 붙여서 서브도메인도 포함
        return if (domain == "localhost") domain else ".$domain"
    }

    fun setCookie(name: String, value: String?) {
        val cookie = Cookie(name, value ?: "").apply {
            path = "/"
            isHttpOnly = true
            domain = cookieDomain()
            secure = true
            setAttribute("SameSite", "Strict")
            maxAge = if (value.isNullOrBlank()) 0 else 60 * 60 * 24 * 365
        }

        resp.addCookie(cookie)
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }

    fun sendRedirect(url: String) {
        resp.sendRedirect(url)
    }
}