package com.example.zipzabe.global.error

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ErrorResponse
import com.example.zipzabe.global.error.exception.ZipzaException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ZipzaException::class)
    fun handleZipzaException(e: ZipzaException): ResponseEntity<ErrorResponse> {
        val errorCode = e.errorCode
        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(ErrorResponse(errorCode.httpStatus, errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(ErrorResponse(errorCode.httpStatus, errorCode.message))
    }
}