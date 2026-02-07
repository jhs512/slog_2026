package com.back.standard.extensions

import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64

// UrlSafe 한 Base64 인코딩
// 모든 문자열 객체에 추가
fun String.base64Encode(): String {
    return Base64.UrlSafe.encode(this.toByteArray(StandardCharsets.UTF_8))
}

// UrlSafe 한 Base64 디코딩
// 모든 문자열 객체에 추가
fun String.base64Decode(): String {
    return Base64.UrlSafe.decode(this).decodeToString()
}