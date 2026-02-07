package com.back.boundedContexts.post.app

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.dto.PostDto
import com.back.boundedContexts.post.dto.PostWithContentDto
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class PostNotificationService(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    fun notifyNewPost(post: Post) {
        messagingTemplate.convertAndSend(
            "/topic/posts/new",
            PostDto(post)
        )
    }

    fun notifyPostModified(post: Post) {
        messagingTemplate.convertAndSend(
            "/topic/posts/${post.id}/modified",
            PostWithContentDto(post)
        )
    }
}
