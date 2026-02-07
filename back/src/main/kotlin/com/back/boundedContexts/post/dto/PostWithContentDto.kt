package com.back.boundedContexts.post.dto

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.postExtensions.commentsCount
import com.back.boundedContexts.post.domain.postExtensions.hitCount
import com.back.boundedContexts.post.domain.postExtensions.likesCount
import java.time.Instant

data class PostWithContentDto(
    val id: Int,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val authorId: Int,
    val authorName: String,
    val authorProfileImgUrl: String,
    val title: String,
    val content: String,
    val published: Boolean,
    val listed: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val hitCount: Int,
    var actorHasLiked: Boolean = false,
    var actorCanModify: Boolean = false,
    var actorCanDelete: Boolean = false,
) {
    constructor(post: Post) : this(
        id = post.id,
        createdAt = post.createdAt,
        modifiedAt = post.modifiedAt,
        authorId = post.author.id,
        authorName = post.author.name,
        authorProfileImgUrl = post.author.redirectToProfileImgUrlOrDefault,
        title = post.title,
        content = post.content,
        published = post.published,
        listed = post.listed,
        likesCount = post.likesCount,
        commentsCount = post.commentsCount,
        hitCount = post.hitCount,
    )
}