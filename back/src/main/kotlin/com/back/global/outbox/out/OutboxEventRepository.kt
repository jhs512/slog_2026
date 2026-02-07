package com.back.global.outbox.out

import com.back.global.outbox.domain.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface OutboxEventRepository : JpaRepository<OutboxEvent, Int> {
    fun findByUid(uid: UUID): OutboxEvent?
}
