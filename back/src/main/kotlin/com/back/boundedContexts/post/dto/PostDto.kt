package com.back.boundedContexts.post.dto

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.postExtensions.commentsCount
import com.back.boundedContexts.post.domain.postExtensions.hitCount
import com.back.boundedContexts.post.domain.postExtensions.likesCount
import com.fasterxml.jackson.annotation.JsonCreator
import java.time.Instant

data class PostDto @JsonCreator constructor(
    val id: Int,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val authorId: Int,
    val authorName: String,
    val authorProfileImgUrl: String,
    val title: String,
    val published: Boolean,
    val listed: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val hitCount: Int,
    var actorHasLiked: Boolean = false,
) {
    constructor(post: Post) : this(
        id = post.id,
        createdAt = post.createdAt,
        modifiedAt = post.modifiedAt,
        authorId = post.author.id,
        authorName = post.author.name,
        authorProfileImgUrl = post.author.redirectToProfileImgUrlOrDefault,
        title = post.title,
        published = post.published,
        listed = post.listed,
        likesCount = post.likesCount,
        commentsCount = post.commentsCount,
        hitCount = post.hitCount,
    )
}