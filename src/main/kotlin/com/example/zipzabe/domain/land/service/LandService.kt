package com.example.zipzabe.domain.land.service

import com.example.zipzabe.domain.land.dto.LandRegisterRequest
import com.example.zipzabe.global.feign.client.ApickClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap

@Service
class LandService(
    private val apickClient: ApickClient,
    @Value("\${apick.auth-key}") private val authKey: String,
) {

    fun getLandRegisterPdf(request: LandRegisterRequest): ByteArray {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("address", request.address)
            add("land_type", request.landType)
            add("bonbun", request.bonbun)
            request.bubun?.let { add("bubun", it) }
        }
        val response = apickClient.getLandRegister(authKey, body)
        return response.body().asInputStream().readBytes()
    }
}
