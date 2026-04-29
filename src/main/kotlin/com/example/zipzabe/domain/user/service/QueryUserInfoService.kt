package com.example.zipzabe.domain.user.service

import com.example.zipzabe.domain.user.dto.UserInfoResponse
import com.example.zipzabe.domain.user.facade.UserFacade
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class QueryUserInfoService(
    private val userFacade: UserFacade
) {
    @Transactional(readOnly = true)
    fun execute(): UserInfoResponse {
        val user = userFacade.getCurrentUser()
        return UserInfoResponse.from(
            email = user.email,
            nickname = user.nickname
        )
    }
}
