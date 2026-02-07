package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["liker_id", "post_id"])
    ]
)
class PostLike(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val liker: Member,
    @field:ManyToOne(fetch = FetchType.LAZY)
    val post: Post,
) : BaseTime()
