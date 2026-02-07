package com.back.global.security.config

import com.back.boundedContexts.member.config.MemberSecurityConfig
import com.back.boundedContexts.post.config.PostSecurityConfig
import com.back.global.app.app.AppFacade
import com.back.global.dto.RsData
import com.back.global.security.config.oauth2.CustomOAuth2AuthorizationRequestResolver
import com.back.global.security.config.oauth2.CustomOAuth2LoginSuccessHandler
import com.back.standard.util.Ut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val customOAuth2LoginSuccessHandler: CustomOAuth2LoginSuccessHandler,
    private val customOAuth2AuthorizationRequestResolver: CustomOAuth2AuthorizationRequestResolver,
    private val postSecurityConfig: PostSecurityConfig,
    private val memberSecurityConfig: MemberSecurityConfig,
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                // ================================
                // 공통
                // ================================
                authorize("/favicon.ico", permitAll)
                authorize("/h2-console/**", permitAll)
                authorize("/gen/**", permitAll)
                authorize("/ws/**", permitAll)

                // ================================
                // 모듈별 설정
                // ================================
                postSecurityConfig.configure(this)
                memberSecurityConfig.configure(this)

                // ================================
                // Admin
                // ================================
                authorize("/member/api/*/adm/**", hasRole("ADMIN"))
                authorize("/post/api/*/adm/**", hasRole("ADMIN"))

                // ================================
                // 기본 규칙
                // ================================
                authorize("/member/api/*/**", authenticated)
                authorize("/post/api/*/**", authenticated)
                authorize(anyRequest, permitAll)
            }

            headers {
                frameOptions { sameOrigin = true }
            }

            csrf { disable() }
            formLogin { disable() }
            logout { disable() }
            httpBasic { disable() }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            oauth2Login {
                authenticationSuccessHandler = customOAuth2LoginSuccessHandler

                authorizationEndpoint {
                    authorizationRequestResolver = customOAuth2AuthorizationRequestResolver
                }
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(customAuthenticationFilter)

            exceptionHandling {
                authenticationEntryPoint = AuthenticationEntryPoint { _, response, _ ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = 401
                    response.writer.write(
                        Ut.JSON.toString(
                            RsData<Void>("401-1", "로그인 후 이용해주세요.")
                        )
                    )
                }

                accessDeniedHandler = AccessDeniedHandler { _, response, _ ->
                    response.contentType = "application/json;charset=UTF-8"
                    response.status = 403
                    response.writer.write(
                        Ut.JSON.toString(
                            RsData<Void>("403-1", "권한이 없습니다.")
                        )
                    )
                }
            }
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("https://cdpn.io", AppFacade.siteFrontUrl)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
            allowCredentials = true
            allowedHeaders = listOf("*")
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/member/api/**", configuration)
            registerCorsConfiguration("/post/api/**", configuration)
        }
    }
}
