package com.example.zipzabe.domain.user.exception

import com.example.zipzabe.global.error.exception.ErrorCode
import com.example.zipzabe.global.error.exception.ZipzaException

object UserNotFoundException : ZipzaException(ErrorCode.USER_NOT_FOUND)