package com.back.boundedContexts.member.subContexts.memberLog.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.jpa.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
class MemberLog(
    val type: String,
    val primaryType: String,
    val primaryId: Int,
    @field:ManyToOne(fetch = FetchType.LAZY) val primaryOwner: Member,
    val secondaryType: String,
    val secondaryId: Int,
    @field:ManyToOne(fetch = FetchType.LAZY) val secondaryOwner: Member,
    @field:ManyToOne(fetch = FetchType.LAZY) val actor: Member,
    @field:Column(columnDefinition = "TEXT") val data: String,
) : BaseEntity() {

}