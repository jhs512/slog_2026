package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.postExtensions.findCommentById
import com.back.boundedContexts.post.domain.postExtensions.getComments
import com.back.boundedContexts.post.dto.PostCommentDto
import com.back.global.dto.RsData
import com.back.global.web.util.Rq
import com.back.standard.extensions.getOrThrow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/post/api/v1/posts/{postId}/comments")
@Tag(name = "ApiV1PostCommentController", description = "API 댓글 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1PostCommentController(
    private val postFacade: PostFacade,
    private val rq: Rq
) {
    val actor
        get() = rq.actor

    private fun makePostCommentDto(postComment: com.back.boundedContexts.post.domain.PostComment): PostCommentDto {
        val actor = rq.actorOrNull
        return PostCommentDto(postComment).apply {
            actorCanModify = postComment.getCheckActorCanModifyRs(actor).isSuccess
            actorCanDelete = postComment.getCheckActorCanDeleteRs(actor).isSuccess
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getItems(
        @PathVariable postId: Int
    ): List<PostCommentDto> {
        val post = postFacade.findById(postId).getOrThrow()

        return post
            .getComments()
            .map { makePostCommentDto(it) }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getItem(
        @PathVariable postId: Int,
        @PathVariable id: Int
    ): PostCommentDto {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = post.findCommentById(id).getOrThrow()

        return makePostCommentDto(postComment)
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    fun delete(
        @PathVariable postId: Int,
        @PathVariable id: Int
    ): RsData<Void> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = post.findCommentById(id).getOrThrow()

        postComment.checkActorCanDelete(rq.actorOrNull)

        postFacade.deleteComment(post, postComment)

        return RsData(
            "200-1",
            "${id}번 댓글이 삭제되었습니다."
        )
    }

    data class PostCommentModifyReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 100)
        val content: String
    )

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    fun modify(
        @PathVariable postId: Int,
        @PathVariable id: Int,
        @Valid @RequestBody reqBody: PostCommentModifyReqBody
    ): RsData<Void> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = post.findCommentById(id).getOrThrow()

        postComment.checkActorCanModify(rq.actorOrNull)

        postFacade.modifyComment(postComment, reqBody.content)

        return RsData(
            "200-1",
            "${id}번 댓글이 수정되었습니다."
        )
    }

    data class PostCommentWriteReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 100)
        val content: String
    )

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    fun write(
        @PathVariable postId: Int,
        @Valid @RequestBody reqBody: PostCommentWriteReqBody
    ): RsData<PostCommentDto> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = postFacade.writeComment(actor, post, reqBody.content)

        return RsData(
            "201-1",
            "${postComment.id}번 댓글이 작성되었습니다.",
            makePostCommentDto(postComment)
        )
    }
}