package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.QPost.post
import com.back.standard.dto.post.type1.PostSearchKeywordType1
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class PostRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PostRepositoryCustom {
    override fun findQPagedByKw(kwType: PostSearchKeywordType1, kw: String, pageable: Pageable): Page<Post> {
        val builder = BooleanBuilder()

        if (kw.isNotBlank()) {
            when (kwType) {
                PostSearchKeywordType1.TITLE -> builder.and(post.title.containsIgnoreCase(kw))
                PostSearchKeywordType1.CONTENT -> builder.and(post.body.content.containsIgnoreCase(kw))
                PostSearchKeywordType1.AUTHOR_NAME -> builder.and(post.author.nickname.containsIgnoreCase(kw))
                PostSearchKeywordType1.ALL ->
                    builder.and(
                        post.title.containsIgnoreCase(kw)
                            .or(post.body.content.containsIgnoreCase(kw))
                            .or(post.author.nickname.containsIgnoreCase(kw))
                    )
            }
        }

        val query = queryFactory
            .selectFrom(post)
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "id" -> post.id
                "authorName" -> post.author.nickname
                else -> null
            }
        }

        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalQuery = queryFactory
            .select(post.count())
            .from(post)
            .where(builder)

        return PageableExecutionUtils.getPage(results, pageable) {
            totalQuery.fetchFirst() ?: 0L
        }
    }
}