package com.example.zipzabe.domain.user.dto

data class UserInfoResponse(
    val email: String,
    val nickname: String
) {
    companion object {
        fun from(email: String, nickname: String): UserInfoResponse {
            return UserInfoResponse(
                email = email,
                nickname = nickname
            )
        }
    }
}
