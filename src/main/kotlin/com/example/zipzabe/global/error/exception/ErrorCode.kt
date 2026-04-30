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

    // Analysis
    ANALYSIS_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Analysis Request Not Found"),
    BUILDING_LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "Building Ledger Not Found"),
    REGISTRY_RAW_NOT_FOUND(HttpStatus.NOT_FOUND, "Registry Raw Not Found"),
    REGISTRY_TITLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Registry Title Not Found"),
    PUBLIC_LEDGER_SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "Public Ledger Summary Not Found"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid Token"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token Expired"),
    BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "Blacklisted Token"),

    // External API
    EXTERNAL_API_NOT_FOUND(HttpStatus.NOT_FOUND, "External API Resource Not Found"),
    EXTERNAL_API_BAD_REQUEST(HttpStatus.BAD_REQUEST, "External API Bad Request"),
    EXTERNAL_API_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "External API Unauthorized"),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "External API Error")
}
