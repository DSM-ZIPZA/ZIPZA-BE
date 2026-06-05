package com.example.zipzabe.domain.registry.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.registry.dto.RegistryApickOcrRequest
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.example.zipzabe.global.feign.client.ApickClient
import feign.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap

@Service
class ApickRegistryPdfService(
    private val apickClient: ApickClient,
    @Value("\${apick.auth-key}") private val authKey: String,
) {
    private val log = LoggerFactory.getLogger(ApickRegistryPdfService::class.java)

    fun issueAndDownloadPdf(analysisRequest: AnalysisRequest, request: RegistryApickOcrRequest): ByteArray {
        var issueAddress = ""
        var issueUniqueNumber = ""
        var issueType = ""
        val issueBody = LinkedMultiValueMap<String, Any>().apply {
            val uniqueNumber = request.uniqueNum?.trim().orEmpty()
            val address = request.address?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: buildAddress(analysisRequest)
            val type = request.type?.takeIf { it.isNotBlank() } ?: defaultRegistryType(analysisRequest)

            if (uniqueNumber.isNotBlank()) {
                add("unique_num", uniqueNumber)
                issueUniqueNumber = uniqueNumber
            } else if (address.isNotBlank()) {
                add("address", address)
                issueAddress = address
            } else {
                throw ExternalApiBadRequestException()
            }

            add("type", type)
            issueType = type
        }

        log.info(
            "Requesting Apick registry issue. requestId={} propertyId={} addressPresent={} uniqueNumPresent={} type={} address={}",
            analysisRequest.id,
            analysisRequest.property.id,
            issueAddress.isNotBlank(),
            issueUniqueNumber.isNotBlank(),
            issueType,
            issueAddress,
        )
        val issueResponse = apickClient.issueRealEstateRegistry(authKey, issueBody)
        val icId = issueResponse.data?.icId ?: throw ExternalApiException()
        if (issueResponse.api?.success == false || issueResponse.data.success == 0) {
            throw ExternalApiException()
        }
        log.info("Apick registry issue accepted. requestId={} icId={}", analysisRequest.id, icId)

        return downloadPdfWithPolling(icId)
    }

    private fun downloadPdfWithPolling(icId: Long): ByteArray {
        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("ic_id", icId.toString())
                add("format", "pdf")
            }
            val response = apickClient.downloadRealEstateRegistry(authKey, body)
            val bytes = response.body()?.asInputStream()?.use { it.readBytes() } ?: ByteArray(0)

            if (isPdf(response, bytes)) {
                log.info("Apick registry PDF downloaded. icId={} attempt={} bytes={}", icId, attempt + 1, bytes.size)
                return bytes
            }

            if (isProcessing(response, bytes) && attempt < MAX_DOWNLOAD_ATTEMPTS - 1) {
                Thread.sleep(DOWNLOAD_RETRY_DELAY_MILLIS)
                return@repeat
            }

            throw ExternalApiException()
        }

        throw ExternalApiException()
    }

    private fun isPdf(response: Response, bytes: ByteArray): Boolean {
        val contentType = response.headerValue("content-type")
        return contentType?.contains("pdf", ignoreCase = true) == true ||
            bytes.take(4).toByteArray().decodeToString() == "%PDF"
    }

    private fun isProcessing(response: Response, bytes: ByteArray): Boolean {
        val result = response.headerValue("result")
        if (result == "2") {
            return true
        }

        val bodyText = runCatching { bytes.decodeToString() }.getOrDefault("")
        return bodyText.contains("처리중") || bodyText.contains("\"result\":2") || bodyText.contains("\"result\": 2")
    }

    private fun Response.headerValue(name: String): String? =
        headers()[name]?.firstOrNull()
            ?: headers()[name.lowercase()]?.firstOrNull()
            ?: headers()[name.uppercase()]?.firstOrNull()

    private fun buildAddress(request: AnalysisRequest): String =
        listOfNotNull(
            request.property.roadAddress.ifBlank { request.property.jibunAddress }
                .takeIf { it.isNotBlank() },
            request.property.detailAddress?.takeIf { it.isNotBlank() },
        ).joinToString(" ")

    private fun defaultRegistryType(request: AnalysisRequest): String =
        if (request.property.isApartment) "집합건물" else "건물"

    companion object {
        private const val MAX_DOWNLOAD_ATTEMPTS = 10
        private const val DOWNLOAD_RETRY_DELAY_MILLIS = 5_000L
    }
}
