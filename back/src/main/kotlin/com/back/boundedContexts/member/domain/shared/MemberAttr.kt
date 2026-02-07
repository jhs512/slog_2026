package com.back.boundedContexts.member.domain.shared

import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.*
import jakarta.persistence.FetchType.LAZY
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId

@Entity
@DynamicUpdate
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["subject_id", "name"])
    ]
)
class MemberAttr(
    @field:NaturalId
    @field:ManyToOne(fetch = LAZY)
    @field:JoinColumn(name = "subject_id")
    val subject: Member,
    @field:NaturalId
    val name: String,
    @field:Column(name = "val", columnDefinition = "TEXT")
    var value: String,
) : BaseTime()
