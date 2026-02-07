package com.back.global.exceptionHandler.config

import com.back.global.dto.RsData
import com.back.global.exception.app.BusinessException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ignored: NoSuchElementException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                RsData(
                    "404-1",
                    "해당 데이터가 존재하지 않습니다."
                )
            )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(e: ConstraintViolationException): ResponseEntity<RsData<Void>> {
        val message = e.constraintViolations
            .asSequence()
            .map { violation ->
                val path = violation.propertyPath.toString()
                val field = path.split(".", limit = 2).getOrElse(1) { path }

                val bits = violation.messageTemplate.split(".")
                val code = bits.getOrNull(bits.size - 2) ?: "Unknown"

                "$field-$code-${violation.message}"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    message
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(e: MethodArgumentNotValidException): ResponseEntity<RsData<Void>> {
        val message = e.bindingResult
            .allErrors
            .asSequence()
            .filterIsInstance<FieldError>()
            .map { err ->
                "${err.field}-${err.code}-${err.defaultMessage}"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    message
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ignored: HttpMessageNotReadableException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    "요청 본문이 올바르지 않습니다."
                )
            )

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(e: MissingRequestHeaderException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    "%s-%s-%s".format(
                        e.headerName,
                        "NotBlank",
                        e.localizedMessage
                    )
                )
            )

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException::class)
    fun handle(e: BusinessException): ResponseEntity<RsData<Void>> {
        val rsData = e.rsData
        return ResponseEntity
            .status(rsData.statusCode)
            .body(rsData)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handle(e: IllegalArgumentException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    e.message ?: "잘못된 요청입니다."
                )
            )

    @ExceptionHandler(IllegalStateException::class)
    fun handle(e: IllegalStateException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    e.message ?: "잘못된 상태입니다."
                )
            )

    @ExceptionHandler(IllegalAccessException::class)
    fun handle(e: IllegalAccessException): ResponseEntity<RsData<Void>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                RsData(
                    "400-1",
                    e.message ?: "잘못된 접근입니다."
                )
            )
}