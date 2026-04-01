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
    USER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "User Already Exists")
}
