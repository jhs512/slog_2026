package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr

// ================================
// 조회수 관리 (PostAttr 기반)
// ================================

val Post.hitCount: Int
    get() = hitCountAttr?.value?.toIntOrNull() ?: 0

fun Post.incrementHitCount() {
    if (hitCountAttr == null)
        hitCountAttr = PostAttr(this, Post.HIT_COUNT, "1")
    else
        hitCountAttr!!.value = (hitCount + 1).toString()

    Post.postAttrRepository.save(hitCountAttr!!)
}
