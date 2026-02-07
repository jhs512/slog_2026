package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException

// ================================
// 읽기 권한
// ================================

/**
 * 읽기 권한 확인: 미공개 글은 작성자나 관리자만 볼 수 있음
 */
fun Post.canRead(actor: Member?): Boolean {
    if (!published) return actor?.id == author.id || actor?.isAdmin == true
    return true
}

fun Post.checkActorCanRead(actor: Member?) {
    if (!canRead(actor)) throw BusinessException("403-3", "${id}번 글 조회권한이 없습니다.")
}

// ================================
// 수정 권한
// ================================

/**
 * 수정 권한 체크 (RsData 반환)
 */
fun Post.getCheckActorCanModifyRs(actor: Member?): RsData<Void> {
    if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
    if (actor == author) return RsData.OK
    return RsData.fail("403-1", "작성자만 글을 수정할 수 있습니다.")
}

fun Post.checkActorCanModify(actor: Member?) {
    val rs = getCheckActorCanModifyRs(actor)
    if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
}

// ================================
// 삭제 권한
// ================================

/**
 * 삭제 권한 체크 (RsData 반환) - 관리자도 삭제 가능
 */
fun Post.getCheckActorCanDeleteRs(actor: Member?): RsData<Void> {
    if (actor == null) return RsData.fail("401-1", "로그인 후 이용해주세요.")
    if (actor.isAdmin) return RsData.OK
    if (actor == author) return RsData.OK
    return RsData.fail("403-2", "작성자만 글을 삭제할 수 있습니다.")
}

fun Post.checkActorCanDelete(actor: Member?) {
    val rs = getCheckActorCanDeleteRs(actor)
    if (rs.isFail) throw BusinessException(rs.resultCode, rs.msg)
}
