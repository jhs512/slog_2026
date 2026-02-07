package com.back.standard.extensions

// null이면 NoSuchElementException을 발생, null이 아니면 nullable 제거
// 모든 nullable 객체에 추가
fun <T : Any> T?.getOrThrow(): T {
    return this ?: throw NoSuchElementException()
}