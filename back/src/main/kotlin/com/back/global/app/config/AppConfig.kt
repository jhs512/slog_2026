package com.back.global.app.app

import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.io.ClassPathResource

@Configuration
class AppConfig(
    environment: Environment,
) {
    init {
        Companion.environment = environment
    }

    companion object {
        private lateinit var environment: Environment
        private var resourcesSampleDirPath: String? = null

        val isDev: Boolean by lazy { environment.matchesProfiles("dev") }
        val isTest: Boolean by lazy { environment.matchesProfiles("test") }
        val isProd: Boolean by lazy { environment.matchesProfiles("prod") }
        val isNotProd: Boolean by lazy { !isProd }
        val systemMemberApiKey: String by lazy { environment.getProperty("custom.systemMemberApiKey")!! }

        // 사이트 도메인 설정
        val siteCookieDomain: String by lazy { environment.getProperty("custom.site.cookieDomain")!! }
        val siteFrontUrl: String by lazy { environment.getProperty("custom.site.frontUrl")!! }
        val siteBackUrl: String by lazy { environment.getProperty("custom.site.backUrl")!! }

        fun getTempDirPath(): String = System.getProperty("java.io.tmpdir")

        fun getResourcesSampleDirPath(): String {
            if (resourcesSampleDirPath == null) {
                val resource = ClassPathResource("sample")

                resourcesSampleDirPath = if (resource.exists()) {
                    resource.file.absolutePath
                } else {
                    "src/main/resources/sample"
                }
            }

            return resourcesSampleDirPath!!
        }
    }
}
