package com.example.zipzabe.global.auth.oauth2

class KakaoOAuth2UserInfo(private val attributes: Map<String, Any>) {

    val id: String
        get() = attributes["id"].toString()

    val email: String
        get() {
            @Suppress("UNCHECKED_CAST")
            val kakaoAccount = attributes["kakao_account"] as? Map<String, Any> ?: return ""
            return kakaoAccount["email"] as? String ?: ""
        }

    val nickname: String
        get() {
            @Suppress("UNCHECKED_CAST")
            val kakaoAccount = attributes["kakao_account"] as? Map<String, Any> ?: return ""
            val profile = kakaoAccount["profile"] as? Map<String, Any> ?: return ""
            return profile["nickname"] as? String ?: ""
        }
}
