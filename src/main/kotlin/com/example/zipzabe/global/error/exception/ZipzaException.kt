package com.example.zipzabe.global.error.exception

abstract class ZipzaException (
    val errorCode: ErrorCode
) : RuntimeException()
