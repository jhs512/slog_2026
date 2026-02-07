package com.back.boundedContexts.post.domain

import com.back.global.jpa.domain.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Lob
import org.hibernate.annotations.DynamicUpdate

@Entity
@DynamicUpdate
class PostBody(
    @Lob
    var content: String
) : BaseEntity()