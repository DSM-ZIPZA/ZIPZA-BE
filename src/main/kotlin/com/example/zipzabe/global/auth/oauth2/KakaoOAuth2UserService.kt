package com.example.zipzabe.global.auth.oauth2

import com.example.zipzabe.domain.user.entity.User
import com.example.zipzabe.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoOAuth2UserService(
    private val userRepository: UserRepository,
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val kakaoUserInfo = KakaoOAuth2UserInfo(oAuth2User.attributes)

        val user = userRepository.findByProviderAndProviderId("kakao", kakaoUserInfo.id)
            ?.also { it.nickname = kakaoUserInfo.nickname }
            ?: userRepository.save(
                User(
                    email = kakaoUserInfo.email,
                    nickname = kakaoUserInfo.nickname,
                    provider = "kakao",
                    providerId = kakaoUserInfo.id,
                )
            )

        val attributes = oAuth2User.attributes + mapOf("userId" to user.id)
        val nameAttributeKey = userRequest.clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName

        return DefaultOAuth2User(oAuth2User.authorities, attributes, nameAttributeKey)
    }
}
