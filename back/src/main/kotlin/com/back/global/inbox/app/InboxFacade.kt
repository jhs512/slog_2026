package com.back.global.inbox.app

import com.back.global.inbox.domain.InboxEvent
import com.back.global.inbox.domain.InboxStatus
import com.back.global.inbox.out.InboxEventRepository
import com.back.standard.dto.EventPayload
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class InboxFacade(
    private val inboxEventRepository: InboxEventRepository
) {
    fun add(event: EventPayload): InboxEvent {
        return inboxEventRepository.save(
            InboxEvent(
                uid = event.uid,
                eventType = event::class.simpleName!!,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                payload = Ut.JSON.toString(event)
            )
        )
    }

    fun markAsProcessing(inboxEvent: InboxEvent) {
        inboxEvent.status = InboxStatus.PROCESSING
    }

    fun markAsCompleted(inboxEvent: InboxEvent) {
        inboxEvent.status = InboxStatus.COMPLETED
    }

    fun markAsFailed(inboxEvent: InboxEvent) {
        inboxEvent.status = InboxStatus.FAILED
    }
}
