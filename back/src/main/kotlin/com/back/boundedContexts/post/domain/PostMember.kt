package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.HasMember
import com.back.boundedContexts.member.domain.shared.Member.Companion.attrRepository
import com.back.boundedContexts.member.domain.shared.MemberAttr

private const val POSTS_COUNT = "postsCount"
private const val POST_COMMENTS_COUNT = "postCommentsCount"
private const val DEFAULT_COUNT = "0"

interface PostMember : HasMember {
    // ================================
    // Attr 프로퍼티 (캐싱 포함)
    // ================================

    val postsCountAttr: MemberAttr
        get() = member.getOrPutAttr(POSTS_COUNT) {
            attrRepository.findBySubjectAndName(member, POSTS_COUNT)
                ?: MemberAttr(member, POSTS_COUNT, DEFAULT_COUNT)
        }

    val postCommentsCountAttr: MemberAttr
        get() = member.getOrPutAttr(POST_COMMENTS_COUNT) {
            attrRepository.findBySubjectAndName(member, POST_COMMENTS_COUNT)
                ?: MemberAttr(member, POST_COMMENTS_COUNT, DEFAULT_COUNT)
        }

    // ================================
    // Count 프로퍼티
    // ================================

    var postsCount: Int
        get() = postsCountAttr.value.toInt()
        set(value) {
            postsCountAttr.value = value.toString()
            attrRepository.save(postsCountAttr)
        }

    var postCommentsCount: Int
        get() = postCommentsCountAttr.value.toInt()
        set(value) {
            postCommentsCountAttr.value = value.toString()
            attrRepository.save(postCommentsCountAttr)
        }

    // ================================
    // Increment/Decrement
    // ================================

    fun incrementPostsCount() {
        postsCount++
    }

    fun decrementPostsCount() {
        postsCount--
    }

    fun incrementPostCommentsCount() {
        postCommentsCount++
    }

    fun decrementPostCommentsCount() {
        postCommentsCount--
    }
}
