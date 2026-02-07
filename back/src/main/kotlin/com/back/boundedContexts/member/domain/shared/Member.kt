package com.back.boundedContexts.member.domain.shared

import com.back.boundedContexts.member.out.shared.MemberAttrRepository
import com.back.boundedContexts.post.domain.PostMember
import com.back.global.jpa.domain.BaseTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

private const val PROFILE_IMG_URL = "profileImgUrl"

@Entity
@DynamicUpdate
class Member(
    id: Int,
    @field:NaturalId
    @field:Column(unique = true)
    val username: String,
    var password: String? = null,
    var nickname: String,
    @field:Column(unique = true)
    var apiKey: String,
) : BaseTime(id), PostMember {

    // ================================
    // Companion Object
    // ================================

    companion object {
        lateinit var attrRepository_: MemberAttrRepository
        val attrRepository by lazy { attrRepository_ }

        val SYSTEM = Member(1, "system", "시스템")

        fun genApiKey() = UUID.randomUUID().toString()
    }

    // ================================
    // Constructors
    // ================================

    // 코프링에서 엔티티의 `by lazy` 필드가 제대로 작동하게 하려면
    // kotlin("plugin.jpa") 에 의해서 만들어지는 인자 없는 생성자로는 부족하다.
    // 귀찮지만 이렇게 직접 만들어야 한다.
    constructor() : this(0)

    constructor(id: Int) : this(id, "", "")

    constructor(id: Int, username: String, nickname: String) : this(
        id,
        username,
        null,
        nickname,
        ""
    )

    constructor(username: String, password: String?, nickname: String) : this(
        0,
        username,
        password,
        nickname,
        genApiKey(),
    )

    // ================================
    // 인터페이스(PostMember 등) 구현을 위한 속성
    // ================================
    override val member: Member get() = this

    // ================================
    // 가상 속성 (Profile)
    // ================================
    override val name: String
        get() = nickname

    @delegate:Transient
    private val profileImgUrlAttr by lazy {
        attrRepository.findBySubjectAndName(this, PROFILE_IMG_URL)
            ?: MemberAttr(this, PROFILE_IMG_URL, "")
    }

    var profileImgUrl: String
        get() = profileImgUrlAttr.value
        set(value) {
            profileImgUrlAttr.value = value
            attrRepository.save(profileImgUrlAttr)
        }

    val profileImgUrlOrDefault: String
        get() = profileImgUrl
            .takeIf { it.isNotBlank() }
            ?: "https://placehold.co/600x600?text=U_U"

    val redirectToProfileImgUrlOrDefault: String
        get() = "http://localhost:8080/member/api/v1/members/${id}/redirectToProfileImg"

    // ================================
    // Member 전용 메서드
    // ================================

    fun modify(nickname: String, profileImgUrl: String?) {
        this.nickname = nickname
        profileImgUrl?.let { this.profileImgUrl = it }
    }

    fun modifyApiKey(apiKey: String) {
        this.apiKey = apiKey
    }

    // ================================
    // Security 영역
    // ================================

    @delegate:Transient
    val isAdmin: Boolean by lazy {
        username in setOf("system", "admin")
    }

    val authoritiesAsStringList: List<String>
        get() = buildList { if (isAdmin) add("ROLE_ADMIN") }

    val authorities: Collection<GrantedAuthority>
        get() = authoritiesAsStringList.map { SimpleGrantedAuthority(it) }
}

