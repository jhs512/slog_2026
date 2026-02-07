package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.domain.PostComment

// ================================
// 댓글 관리 (PostAttr + Repository 기반)
// ================================

val Post.commentsCount: Int
    get() = commentsCountAttr?.value?.toIntOrNull() ?: 0

private fun Post.setCommentsCount(value: Int) {
    if (commentsCountAttr == null)
        commentsCountAttr = PostAttr(this, Post.COMMENTS_COUNT, value.toString())
    else
        commentsCountAttr!!.value = value.toString()

    Post.postAttrRepository.save(commentsCountAttr!!)
}

fun Post.getComments(): List<PostComment> =
    Post.postCommentRepository.findByPostOrderByIdDesc(this)

fun Post.findCommentById(id: Int): PostComment? =
    Post.postCommentRepository.findByPostAndId(this, id)

fun Post.addComment(author: Member, content: String): PostComment {
    val postComment = PostComment(author, this, content)
    Post.postCommentRepository.save(postComment)

    setCommentsCount(commentsCount + 1)
    author.incrementPostCommentsCount()

    return postComment
}

fun Post.deleteComment(postComment: PostComment) {
    postComment.author.decrementPostCommentsCount()
    setCommentsCount(commentsCount - 1)

    Post.postCommentRepository.delete(postComment)
}
