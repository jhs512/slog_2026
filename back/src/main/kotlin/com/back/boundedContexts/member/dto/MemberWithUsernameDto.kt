package com.back.boundedContexts.member.dto

import com.back.boundedContexts.member.domain.shared.Member
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class MemberWithUsernameDto @JsonCreator constructor(
    val id: Int,
    val createdAt: Instant,
    val modifiedAt: Instant,
    @get:JsonProperty("isAdmin")
    val isAdmin: Boolean,
    val username: String,
    val name: String,
    val profileImageUrl: String,
) {
    constructor(member: Member) : this(
        member.id,
        member.createdAt,
        member.modifiedAt,
        member.isAdmin,
        member.username,
        member.name,
        member.redirectToProfileImgUrlOrDefault
    )
}
