package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.domain.shared.QMember
import com.back.standard.dto.member.type1.MemberSearchKeywordType1
import com.back.standard.util.QueryDslUtil
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class MemberRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val entityManager: EntityManager,
) : MemberRepositoryCustom {
    override fun findByUsername(username: String): Member? {
        return entityManager.unwrap(Session::class.java)
            .byNaturalId(Member::class.java)
            .using(Member::username.name, username)
            .load()
    }

    override fun findQPagedByKw(
        kwType: MemberSearchKeywordType1,
        kw: String,
        pageable: Pageable
    ): Page<Member> {
        val member = QMember.member

        val builder = BooleanBuilder()

        if (kw.isNotBlank()) {
            when (kwType) {
                MemberSearchKeywordType1.USERNAME -> builder.and(member.username.containsIgnoreCase(kw))
                MemberSearchKeywordType1.NICKNAME -> builder.and(member.nickname.containsIgnoreCase(kw))
                MemberSearchKeywordType1.ALL ->
                    builder.and(
                        member.username.containsIgnoreCase(kw)
                            .or(member.nickname.containsIgnoreCase(kw))
                    )
            }
        }

        val query = queryFactory
            .selectFrom(member)
            .where(builder)

        QueryDslUtil.applySorting(query, pageable) { property ->
            when (property) {
                "id" -> member.id
                "username" -> member.username
                "nickname" -> member.nickname
                else -> null
            }
        }

        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalQuery = queryFactory
            .select(member.count())
            .from(member)
            .where(builder)

        return PageableExecutionUtils.getPage(results, pageable) {
            totalQuery.fetchFirst() ?: 0L
        }
    }
}
