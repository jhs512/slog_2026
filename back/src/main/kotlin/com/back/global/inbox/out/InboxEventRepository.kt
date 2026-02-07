package com.back.global.inbox.out

import com.back.global.inbox.domain.InboxEvent
import org.springframework.data.jpa.repository.JpaRepository

interface InboxEventRepository : JpaRepository<InboxEvent, Int>
