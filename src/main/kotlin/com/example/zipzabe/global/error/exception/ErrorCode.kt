package com.example.zipzabe.global.error.exception

import org.springframework.http.HttpStatus

enum class ErrorCode (
    val httpStatus: HttpStatus,
    val message: String
)
{
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User Not Found"),
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "User Already Exists"),

    // External API
    EXTERNAL_API_NOT_FOUND(HttpStatus.NOT_FOUND, "External API Resource Not Found"),
    EXTERNAL_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "External API Bad Request"),
    EXTERNAL_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "External API Unauthorized"),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "External API Error")
}
