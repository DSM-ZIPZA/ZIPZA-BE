package com.example.zipzabe.domain.building.service

import com.example.zipzabe.domain.building.dto.BuildingRegisterListResponse
import com.example.zipzabe.domain.building.dto.BuildingRegisterRequest
import com.example.zipzabe.global.feign.client.ApickClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap

@Service
class BuildingService(
    private val apickClient: ApickClient,
    @Value("\${apick.auth-key}") private val authKey: String,
) {

    fun getBuildingRegisterPdf(request: BuildingRegisterRequest): ByteArray {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("address", request.address)
            add("b_name", request.bName)
            add("dong", request.dong)
            add("ho", request.ho)
        }
        val response = apickClient.getBuildingRegister(authKey, body)
        return response.body().asInputStream().readBytes()
    }

    fun getBuildingRegisterList(address: String): BuildingRegisterListResponse {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("address", address)
        }
        return apickClient.getBuildingRegisterList(authKey, body)
    }
}
