package com.back.global.task.domain

import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.DynamicUpdate
import java.time.Instant
import java.util.*
import kotlin.math.pow

enum class TaskStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}

@Entity
@DynamicUpdate
class Task(
    @field:Column(unique = true)
    val uid: UUID,
    val aggregateType: String,
    val aggregateId: Int,
    val taskType: String,
    @Column(columnDefinition = "TEXT")
    val payload: String,
    @Enumerated(EnumType.STRING)
    var status: TaskStatus = TaskStatus.PENDING,
    var retryCount: Int = 0,
    var maxRetries: Int = 10,
    var nextRetryAt: Instant = Instant.now(),
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null
) : BaseTime() {

    fun scheduleRetry() {
        retryCount++

        if (retryCount >= maxRetries) {
            status = TaskStatus.FAILED
        } else {
            status = TaskStatus.PENDING
            // 60초 * 3^retryCount (60s, 180s, 540s, ...)
            val delaySeconds = 60 * 3.0.pow(retryCount.toDouble()).toLong()
            nextRetryAt = Instant.now().plusSeconds(delaySeconds)
        }
    }

    fun markAsCompleted() {
        status = TaskStatus.COMPLETED
    }

    fun markAsProcessing() {
        status = TaskStatus.PROCESSING
    }
}
