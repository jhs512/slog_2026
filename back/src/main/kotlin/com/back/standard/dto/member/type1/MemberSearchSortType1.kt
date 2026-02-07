package com.back.standard.dto.member.type1

import com.back.standard.extensions.toCamelCase
import org.springframework.data.domain.Sort

enum class MemberSearchSortType1 {
    ID,
    ID_ASC,
    USERNAME,
    USERNAME_ASC,
    NICKNAME,
    NICKNAME_ASC;

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