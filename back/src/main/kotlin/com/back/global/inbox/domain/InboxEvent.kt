package com.back.global.inbox.domain

import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.DynamicUpdate
import java.util.*

enum class InboxStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

@Entity
@DynamicUpdate
class InboxEvent(
    @field:Column(unique = true)
    val uid: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: Int,
    @Column(columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    var status: InboxStatus = InboxStatus.PENDING
) : BaseTime()
