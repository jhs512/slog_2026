package com.back.boundedContexts.member.subContexts.memberLog.dto

import com.back.standard.dto.EventPayload
import com.back.standard.dto.TaskPayload
import java.util.*

class MemberAddLogPayload(
    override val uid: UUID,
    override val aggregateType: String,
    override val aggregateId: Int,
    val event: EventPayload,
) : TaskPayload