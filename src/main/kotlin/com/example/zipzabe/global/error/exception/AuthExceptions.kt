package com.example.zipzabe.global.error.exception

class InvalidTokenException : ZipzaException(ErrorCode.INVALID_TOKEN)
class TokenExpiredException : ZipzaException(ErrorCode.TOKEN_EXPIRED)
class BlacklistedTokenException : ZipzaException(ErrorCode.BLACKLISTED_TOKEN)
