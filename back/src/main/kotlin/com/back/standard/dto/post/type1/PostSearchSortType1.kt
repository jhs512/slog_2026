package com.back.standard.dto.post.type1

import com.back.standard.extensions.toCamelCase
import org.springframework.data.domain.Sort

enum class PostSearchSortType1 {
    ID,
    ID_ASC,
    AUTHOR_NAME,
    AUTHOR_NAME_ASC;

    val sortBy: Sort by lazy {
        Sort.by(
            if (isAsc) Sort.Direction.ASC else Sort.Direction.DESC,
            property
        )
    }

    val property by lazy {
        name.removeSuffix("_ASC").toCamelCase()
    }

    val isAsc by lazy {
        name.endsWith("_ASC")
    }
}