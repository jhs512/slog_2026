package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository
import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
class Post(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    var title: String,
    content: String,
    var published: Boolean = false,
    var listed: Boolean = false,
) : BaseTime() {
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var body: PostBody = PostBody(content)

    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var likesCountAttr: PostAttr? = null

    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var commentsCountAttr: PostAttr? = null

    @field:OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.REMOVE])
    var hitCountAttr: PostAttr? = null

    // ================================
    // Companion Object
    // ================================

    companion object {
        lateinit var postAttrRepository_: PostAttrRepository
        val postAttrRepository by lazy { postAttrRepository_ }

        lateinit var postLikeRepository_: PostLikeRepository
        val postLikeRepository by lazy { postLikeRepository_ }

        lateinit var postCommentRepository_: PostCommentRepository
        val postCommentRepository by lazy { postCommentRepository_ }

        // Attr 이름 상수
        const val LIKES_COUNT = "likesCount"
        const val COMMENTS_COUNT = "commentsCount"
        const val HIT_COUNT = "hitCount"
    }

    // ================================
    // 기본 데이터 조작
    // ================================

    var content: String
        get() = body.content
        set(value) {
            if (body.content != value) {
                body.content = value
                updateModifiedAt()
            }
        }

    fun modify(title: String, content: String, published: Boolean? = null, listed: Boolean? = null) {
        this.title = title
        this.content = content
        published?.let { this.published = it }
        listed?.let { this.listed = it }
    }
}