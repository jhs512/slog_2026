package com.back.global.outbox.app

import com.back.global.outbox.domain.OutboxEvent
import com.back.global.outbox.domain.OutboxStatus
import com.back.global.outbox.out.OutboxEventRepository
import com.back.standard.dto.EventPayload
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class OutboxFacade(
    private val outboxEventRepository: OutboxEventRepository
) {
    fun add(event: EventPayload): OutboxEvent {
        return outboxEventRepository.save(
            OutboxEvent(
                event.uid,
                event::class.simpleName!!,
                event.aggregateType,
                event.aggregateId,
                payload = Ut.JSON.toString(event)
            )
        )
    }

    fun markAsProcessing(outboxEvent: OutboxEvent) {
        outboxEvent.status = OutboxStatus.PROCESSING
    }

    fun markAsCompleted(outboxEvent: OutboxEvent) {
        outboxEvent.status = OutboxStatus.COMPLETED
    }

    fun markAsFailed(outboxEvent: OutboxEvent) {
        outboxEvent.status = OutboxStatus.FAILED
    }
}
