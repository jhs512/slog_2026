package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import jakarta.persistence.EntityManager
import org.hibernate.Session

class PostAttrRepositoryImpl(
    private val entityManager: EntityManager,
) : PostAttrRepositoryCustom {
    override fun findBySubjectAndName(subject: Post, name: String): PostAttr? {
        return entityManager.unwrap(Session::class.java)
            .byNaturalId(PostAttr::class.java)
            .using(PostAttr::subject.name, subject)
            .using(PostAttr::name.name, name)
            .load()
    }
}
