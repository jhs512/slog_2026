package com.back.boundedContexts.member.subContexts.memberLog.app

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.subContexts.memberLog.domain.MemberLog
import com.back.boundedContexts.member.subContexts.memberLog.out.MemberLogRepository
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.event.PostCommentWrittenEvent
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class MemberLogFacade(
    private val memberLogRepository: MemberLogRepository,
) {
    fun save(event: PostCommentWrittenEvent) {
        val log = MemberLog(
            PostCommentWrittenEvent::class.simpleName!!,
            PostComment::class.simpleName!!,
            event.postCommentDto.id,
            Member(event.postCommentDto.authorId),
            Post::class.simpleName!!,
            event.postDto.id,
            Member(event.postDto.authorId),
            Member(event.actorDto.id),
            Ut.JSON.toString(event)
        )

        memberLogRepository.save(log)
    }
}
