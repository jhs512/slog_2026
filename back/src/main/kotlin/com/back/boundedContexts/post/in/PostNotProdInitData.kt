package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.global.app.app.CustomConfigProperties
import com.back.standard.extensions.getOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.transaction.annotation.Transactional

@Profile("!prod")
@Configuration
class PostNotProdInitData(
    private val memberFacade: MemberFacade,
    private val postFacade: PostFacade,
    private val customConfigProperties: CustomConfigProperties,
) {
    @Lazy
    @Autowired
    private lateinit var self: PostNotProdInitData

    @Bean
    @Order(2)
    fun postNotProdInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner {
            self.makeBasePosts()
        }
    }

    @Transactional
    fun makeBasePosts() {
        if (postFacade.count() > 0) return

        val memberUser1 = memberFacade.findByUsername("user1").getOrThrow()
        val memberUser2 = memberFacade.findByUsername("user2").getOrThrow()
        val memberUser3 = memberFacade.findByUsername("user3").getOrThrow()

        val post1 = postFacade.write(memberUser1, "제목 1", "내용 1", published = true, listed = true)
        val post2 = postFacade.write(memberUser2, "제목 2", "내용 2", published = true, listed = true)
        val post3 = postFacade.write(memberUser3, "제목 3", "내용 3", published = true, listed = true)

        postFacade.writeComment(memberUser1, post1, "댓글 1-1")
        postFacade.writeComment(memberUser1, post1, "댓글 1-2")
        postFacade.writeComment(memberUser2, post1, "댓글 1-3")
        postFacade.writeComment(memberUser3, post2, "댓글 2-1")
        postFacade.writeComment(memberUser3, post2, "댓글 2-2")
    }
}