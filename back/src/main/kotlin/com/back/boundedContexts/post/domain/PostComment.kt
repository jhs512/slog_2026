package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
class PostComment(
    @field:ManyToOne(fetch = FetchType.LAZY)
    val author: Member,
    @field:ManyToOne(fetch = FetchType.LAZY)
    val post: Post,
    var content: String,
) : BaseTime() {
    fun modify(content: String) {
        this.content = content
    }

    // 수정 권한 체크 (RsData 반환)
    fun getCheckActorCanModifyRs(actor: Member?): RsData<Void> {
        if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
        if (actor == author) return RsData.OK
        return RsData.fail("403-1", "작성자만 댓글을 수정할 수 있습니다.")
    }

    fun checkActorCanModify(actor: Member?) {
        val rs = getCheckActorCanModifyRs(actor)
        if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
    }

    // 삭제 권한 체크 (RsData 반환) - 관리자도 삭제 가능
    fun getCheckActorCanDeleteRs(actor: Member?): RsData<Void> {
        if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
        if (actor.isAdmin) return RsData.OK
        if (actor == author) return RsData.OK
        return RsData.fail("403-2", "작성자만 댓글을 삭제할 수 있습니다.")
    }

    fun checkActorCanDelete(actor: Member?) {
        val rs = getCheckActorCanDeleteRs(actor)
        if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
    }
}
