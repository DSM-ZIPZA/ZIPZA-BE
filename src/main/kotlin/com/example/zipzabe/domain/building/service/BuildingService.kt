package com.example.zipzabe.domain.building.service

import com.example.zipzabe.domain.building.dto.BuildingRegisterListResponse
import com.example.zipzabe.domain.building.dto.BuildingRegisterRequest
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.example.zipzabe.global.error.exception.ExternalApiNotFoundException
import com.example.zipzabe.global.feign.client.ApickClient
import feign.Response
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
        ensureSuccessfulPdfResponse(response)
        return response.body()?.asInputStream()?.use { it.readBytes() } ?: throw ExternalApiException()
    }

    fun getBuildingRegisterList(address: String): BuildingRegisterListResponse {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("address", address)
        }
        return apickClient.getBuildingRegisterList(authKey, body)
    }

    private fun ensureSuccessfulPdfResponse(response: Response) {
        when (response.status()) {
            in 200..299 -> return
            400 -> throw ExternalApiBadRequestException()
            404 -> throw ExternalApiNotFoundException()
            else -> throw ExternalApiException()
        }
    }
}
