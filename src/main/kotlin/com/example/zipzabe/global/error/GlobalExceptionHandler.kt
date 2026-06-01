package com.example.zipzabe.global.error

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ErrorResponse
import com.example.zipzabe.global.error.exception.ZipzaException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ZipzaException::class)
    fun handleZipzaException(e: ZipzaException, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errorCode = e.errorCode
        log.warn(
            "Handled ZipzaException. method={} uri={} query={} status={} errorCode={} remoteAddr={} userAgent={}",
            request.method,
            request.requestURI,
            request.queryString,
            errorCode.httpStatus.value(),
            errorCode.name,
            request.remoteAddr,
            request.getHeader("User-Agent"),
            e,
        )
        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(ErrorResponse(errorCode.httpStatus, errorCode.message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR
        log.error(
            "Unhandled exception. method={} uri={} query={} status={} remoteAddr={} userAgent={}",
            request.method,
            request.requestURI,
            request.queryString,
            errorCode.httpStatus.value(),
            request.remoteAddr,
            request.getHeader("User-Agent"),
            e,
        )
        return ResponseEntity
            .status(errorCode.httpStatus)
            .body(ErrorResponse(errorCode.httpStatus, errorCode.message))
    }
}
