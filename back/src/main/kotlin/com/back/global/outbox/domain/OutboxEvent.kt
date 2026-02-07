package com.back.global.outbox.domain

import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.DynamicUpdate
import java.util.*

enum class OutboxStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

@Entity
@DynamicUpdate
class OutboxEvent(
    @field:Column(unique = true)
    val uid: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: Int,
    @Column(columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    var status: OutboxStatus = OutboxStatus.PENDING
) : BaseTime()
