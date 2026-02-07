package com.back.boundedContexts.post.app

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.domain.postExtensions.addComment
import com.back.boundedContexts.post.domain.postExtensions.deleteComment
import com.back.boundedContexts.post.dto.PostCommentDto
import com.back.boundedContexts.post.dto.PostDto
import com.back.boundedContexts.post.event.PostCommentWrittenEvent
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository
import com.back.boundedContexts.post.out.PostRepository
import com.back.global.event.app.EventPublisher
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class PostFacade(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postAttrRepository: PostAttrRepository,
    private val postCommentRepository: PostCommentRepository,
    private val eventPublisher: EventPublisher,
    private val postNotificationService: PostNotificationService,
) {
    fun count(): Long = postRepository.count()

    fun write(
        author: Member,
        title: String,
        content: String,
        published: Boolean = false,
        listed: Boolean = false,
    ): Post {
        val post = Post(
            author = author,
            title = title,
            content = content,
            published = published,
            listed = listed,
        )

        author.incrementPostsCount()

        return postRepository.save(post)
    }


    fun findById(id: Int): Post? = postRepository.findById(id).getOrNull()


    fun modify(
        post: Post,
        title: String,
        content: String,
        published: Boolean? = null,
        listed: Boolean? = null,
    ) {
        val wasPublishedAndListed = post.published && post.listed

        post.modify(title, content, published, listed)

        val isNowPublishedAndListed = post.published && post.listed

        // 처음 공개될 때만 알림
        if (!wasPublishedAndListed && isNowPublishedAndListed) {
            postNotificationService.notifyNewPost(post)
        }

        // 글 본문 변경사항 구독자에게 알림
        postNotificationService.notifyPostModified(post)
    }

    fun findPagedByAuthor(
        author: Member,
        page: Int,
        pageSize: Int,
    ): Page<Post> = postRepository.findByAuthorOrderByIdDesc(
        author,
        PageRequest.of(page - 1, pageSize)
    )

    fun findTemp(author: Member) = postRepository.findFirstByAuthorAndTitleAndPublishedFalseOrderByIdAsc(author, "임시글")

    /**
     * 임시저장 글 조회 또는 생성
     * @return Pair(Post, isNew) - 기존 글이면 false, 새로 생성이면 true
     */
    fun getOrCreateTemp(author: Member): Pair<Post, Boolean> {
        val existingTemp = findTemp(author)

        if (existingTemp != null) {
            return existingTemp to false
        }

        val newPost = Post(
            author,
            "임시글",
            "임시글 입니다.",
        )

        author.incrementPostsCount()

        return postRepository.save(newPost) to true
    }


    fun writeComment(author: Member, post: Post, content: String): PostComment {
        val postComment = post.addComment(author, content)

        postRepository.flush()

        eventPublisher.publish(
            PostCommentWrittenEvent(
                UUID.randomUUID(),
                PostCommentDto(postComment),
                PostDto(post),
                MemberDto(author)
            )
        )

        return postComment
    }


    fun deleteComment(post: Post, postComment: PostComment) =
        post.deleteComment(postComment)


    fun modifyComment(postComment: PostComment, content: String) =
        postComment.modify(content)


    fun delete(post: Post) {
        post.author.decrementPostsCount()

        // OneToOne 참조 해제 (Hibernate flush 순서 문제 방지)
        post.likesCountAttr = null
        post.commentsCountAttr = null
        post.hitCountAttr = null
        postRepository.flush()

        // 연관 데이터 먼저 삭제 (FK 제약 조건 방지)
        postAttrRepository.deleteBySubject(post)
        postCommentRepository.deleteByPost(post)
        postLikeRepository.deleteByPost(post)
        postRepository.flush()

        postRepository.delete(post)
    }


    fun findLatest() = postRepository.findFirstByOrderByIdDesc()


    fun findPagedByKw(
        kwType: PostSearchKeywordType1,
        kw: String,
        sort: PostSearchSortType1,
        page: Int,
        pageSize: Int
    ): Page<Post> =
        postRepository.findQPagedByKw(
            kwType,
            kw,
            PageRequest.of(
                page - 1,
                pageSize,
                sort.sortBy
            )
        )

    fun findLikedPostIds(liker: Member?, posts: List<Post>): Set<Int> {
        if (liker == null || posts.isEmpty()) return emptySet()

        return postLikeRepository
            .findByLikerAndPostIn(liker, posts)
            .map { it.post.id }
            .toSet()
    }
}
