package com.back.global.event.app

import com.back.global.app.app.AppFacade
import com.back.global.inbox.app.InboxFacade
import com.back.global.outbox.app.OutboxFacade
import com.back.standard.dto.EventPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service


/**
 * 도메인 이벤트 발행 서비스.
 **/
@Service
class EventPublisher(
    private val outboxFacade: OutboxFacade,
    private val inboxFacade: InboxFacade,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun publish(event: EventPayload) {
        val outboxEvent = outboxFacade.add(event)
        val inboxEvent = inboxFacade.add(event)

        if (AppFacade.isProd) return

        outboxFacade.markAsProcessing(outboxEvent)
        inboxFacade.markAsProcessing(inboxEvent)

        applicationEventPublisher.publishEvent(event)

        outboxFacade.markAsCompleted(outboxEvent)
        inboxFacade.markAsCompleted(inboxEvent)
    }
}
