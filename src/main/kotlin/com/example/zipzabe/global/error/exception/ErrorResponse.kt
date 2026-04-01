package com.example.zipzabe.global.error.exception

import org.springframework.http.HttpStatus

data class ErrorResponse(
    val status: HttpStatus,
    val message: String?
)
