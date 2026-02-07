package com.back.boundedContexts.member.subContexts.memberLog.out

import com.back.boundedContexts.member.subContexts.memberLog.domain.MemberLog
import org.springframework.data.jpa.repository.JpaRepository

interface MemberLogRepository : JpaRepository<MemberLog, Int> {

}
