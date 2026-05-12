package com.example.zipzabe.global.error.exception

class ExternalApiException : ZipzaException(ErrorCode.EXTERNAL_API_ERROR)

class ExternalApiBadRequestException : ZipzaException(ErrorCode.EXTERNAL_API_BAD_REQUEST)
