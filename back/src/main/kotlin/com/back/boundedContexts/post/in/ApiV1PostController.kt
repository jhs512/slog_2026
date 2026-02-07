package com.back.boundedContexts.post.`in`

import PageDto
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.postExtensions.*
import com.back.boundedContexts.post.dto.PostDto
import com.back.boundedContexts.post.dto.PostWithContentDto
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import com.back.global.web.util.Rq
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.dto.post.type1.PostSearchSortType1
import com.back.standard.extensions.getOrThrow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/post/api/v1/posts")
@Tag(name = "ApiV1PostController", description = "API 글 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1PostController(
    private val postFacade: PostFacade,
    private val rq: Rq
) {
    private fun makePostWithContentDto(post: Post): PostWithContentDto {
        val actor = rq.actorOrNull

        return PostWithContentDto(post).apply {
            actorHasLiked = post.isLikedBy(actor)
            actorCanModify = post.getCheckActorCanModifyRs(actor).isSuccess
            actorCanDelete = post.getCheckActorCanDeleteRs(actor).isSuccess
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getItems(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
        @RequestParam(defaultValue = "ALL") kwType: PostSearchKeywordType1,
        @RequestParam(defaultValue = "") kw: String,
        @RequestParam(defaultValue = "ID") sort: PostSearchSortType1,
    ): PageDto<PostDto> {
        val page: Int = if (page >= 1) {
            page
        } else {
            1
        }

        val pageSize: Int = if (pageSize in 1..30) {
            pageSize
        } else {
            30
        }

        val postPage = postFacade.findPagedByKw(
            kwType,
            kw,
            sort,
            page,
            pageSize
        )

        val actor = rq.actorOrNull
        val likedPostIds = postFacade.findLikedPostIds(actor, postPage.content)

        return PageDto(
            postPage
                .map {
                    PostDto(it).apply {
                        actorHasLiked = it.id in likedPostIds
                    }
                }
        )
    }


    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getItem(
        @PathVariable id: Int,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        lastModifiedAt: Instant?
    ): PostWithContentDto {
        val post = postFacade.findById(id).getOrThrow()

        if (lastModifiedAt != null && !post.modifiedAt.isAfter(lastModifiedAt)) {
            throw BusinessException("412-1", "변경된 데이터가 없습니다.")
        }

        post.checkActorCanRead(rq.actorOrNull)

        return makePostWithContentDto(post)
    }


    data class PostHitResBody(
        val hitCount: Int,
    )

    @PostMapping("/{id}/hit")
    @Transactional
    @Operation(summary = "조회수 증가", description = "클라이언트에서 호출. 중복 체크는 클라이언트 로컬 스토리지에서 처리")
    fun incrementHit(
        @PathVariable id: Int
    ): RsData<PostHitResBody> {
        val post = postFacade.findById(id).getOrThrow()

        post.incrementHitCount()

        return RsData("200-1", "조회수가 증가했습니다.", PostHitResBody(post.hitCount))
    }


    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    fun delete(
        @PathVariable id: Int
    ): RsData<Void> {
        val post = postFacade.findById(id).getOrThrow()

        post.checkActorCanDelete(rq.actor)

        postFacade.delete(post)

        return RsData(
            "200-1",
            "${id}번 글이 삭제되었습니다."
        )
    }


    data class PostWriteReqBody(
        @field:NotBlank
        @field:Size(min = 2, max = 100)
        val title: String,
        @field:NotBlank
        @field:Size(min = 2)
        val content: String,
        val published: Boolean?,
        val listed: Boolean?,
    )

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    fun write(
        @Valid @RequestBody reqBody: PostWriteReqBody
    ): RsData<PostDto> {
        val post = postFacade.write(
            rq.actor,
            reqBody.title,
            reqBody.content,
            reqBody.published ?: false,
            reqBody.listed ?: false,
        )

        return RsData(
            "201-1",
            "${post.id}번 글이 작성되었습니다.",
            PostDto(post)
        )
    }


    data class PostModifyReqBody(
        @field:Size(max = 100)
        val title: String,
        val content: String,
        val published: Boolean? = null,
        val listed: Boolean? = null,
    )

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    fun modify(
        @PathVariable id: Int,
        @Valid @RequestBody reqBody: PostModifyReqBody
    ): RsData<PostDto> {
        val post = postFacade.findById(id).getOrThrow()

        post.checkActorCanModify(rq.actor)

        postFacade.modify(
            post,
            reqBody.title,
            reqBody.content,
            reqBody.published,
            reqBody.listed,
        )

        return RsData(
            "200-1",
            "${post.id}번 글이 수정되었습니다.",
            PostDto(post)
        )
    }


    @GetMapping("/mine")
    @Transactional(readOnly = true)
    @Operation(summary = "내 게시물 목록 조회 (임시저장 포함)")
    fun getMine(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
    ): PageDto<PostDto> {
        val validPage = page.coerceAtLeast(1)
        val validPageSize = pageSize.coerceIn(1, 30)

        val postPage = postFacade.findPagedByAuthor(
            rq.actor,
            validPage,
            validPageSize,
        )

        val likedPostIds = postFacade.findLikedPostIds(rq.actor, postPage.content)

        return PageDto(
            postPage.map { post ->
                PostDto(post).apply {
                    actorHasLiked = post.id in likedPostIds
                }
            }
        )
    }


    @PostMapping("/temp")
    @Transactional
    @Operation(summary = "임시저장 생성/조회", description = "기존 임시저장 글이 있으면 반환, 없으면 새로 생성")
    fun getOrCreateTemp(): RsData<PostWithContentDto> {
        val (post, isNew) = postFacade.getOrCreateTemp(rq.actor)

        return if (isNew) {
            RsData("201-1", "임시저장 글이 생성되었습니다.", makePostWithContentDto(post))
        } else {
            RsData("200-1", "기존 임시저장 글을 반환합니다.", makePostWithContentDto(post))
        }
    }


    data class PostLikeToggleResBody(
        val liked: Boolean,
        val likesCount: Int,
    )

    @PostMapping("/{id}/like")
    @Transactional
    @Operation(summary = "좋아요 토글")
    fun toggleLike(
        @PathVariable id: Int
    ): RsData<PostLikeToggleResBody> {
        val post = postFacade.findById(id).getOrThrow()

        val liked = post.toggleLike(rq.actor)

        val msg = if (liked) "좋아요를 눌렀습니다." else "좋아요를 취소했습니다."

        return RsData(
            "200-1",
            msg,
            PostLikeToggleResBody(liked, post.likesCount)
        )
    }
}
