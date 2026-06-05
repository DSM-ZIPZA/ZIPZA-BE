package com.example.zipzabe.domain.registry.service

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.registry.dto.ApickIrosIssueResponse
import com.example.zipzabe.domain.registry.dto.RegistryApickOcrRequest
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Service
class ApickRegistryPdfService(
    @Value("\${apick.url}") apickUrl: String,
    @Value("\${apick.auth-key}") private val authKey: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(ApickRegistryPdfService::class.java)
    private val restClient = RestClient.builder()
        .baseUrl(apickUrl.trimEnd('/'))
        .build()

    fun issueAndDownloadPdf(analysisRequest: AnalysisRequest, request: RegistryApickOcrRequest): ByteArray {
        var issueAddress = ""
        var issueUniqueNumber = ""
        var issueType = ""
        val issueBody = LinkedMultiValueMap<String, Any>().apply {
            val uniqueNumber = request.uniqueNum?.trim().orEmpty()
            val address = request.address?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { appendDetailAddress(it, analysisRequest.property.detailAddress) }
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
        val issueResponseBytes = postMultipart("/rest/iros/1", issueBody).let { response ->
            ensureSuccessfulResponse(response)
            response.body ?: ByteArray(0)
        }
        throwIfApickJsonError(issueResponseBytes, "registryIssue")
        val issueResponse = runCatching {
            objectMapper.readValue(issueResponseBytes, ApickIrosIssueResponse::class.java)
        }.getOrElse { e ->
            log.warn(
                "Failed to parse Apick registry issue response. requestId={} body={}",
                analysisRequest.id,
                issueResponseBytes.decodeToString().take(MAX_LOG_BODY_CHARS),
                e,
            )
            throw ExternalApiException()
        }
        val icId = issueResponse.data?.icId ?: throw ExternalApiException()
        log.info("Apick registry issue accepted. requestId={} icId={}", analysisRequest.id, icId)

        return downloadPdfWithPolling(icId)
    }

    private fun downloadPdfWithPolling(icId: Long): ByteArray {
        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("ic_id", icId.toString())
                add("format", "pdf")
            }
            val response = postMultipart("/rest/iros_download/1", body)
            val bytes = response.body ?: ByteArray(0)

            if (isPdf(response, bytes)) {
                log.info("Apick registry PDF downloaded. icId={} attempt={} bytes={}", icId, attempt + 1, bytes.size)
                return bytes
            }

            if (isProcessing(response, bytes) && attempt < MAX_DOWNLOAD_ATTEMPTS - 1) {
                Thread.sleep(DOWNLOAD_RETRY_DELAY_MILLIS)
                return@repeat
            }

            ensureSuccessfulResponse(response)
            throwIfApickJsonError(bytes, "registryDownload")
            throw ExternalApiException()
        }

        throw ExternalApiException()
    }

    private fun postMultipart(path: String, body: LinkedMultiValueMap<String, Any>): ResponseEntity<ByteArray> =
        try {
            restClient.post()
                .uri(path)
                .header("CL_AUTH_KEY", authKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(ByteArray::class.java)
        } catch (e: RestClientResponseException) {
            ResponseEntity
                .status(e.statusCode)
                .headers(e.responseHeaders ?: HttpHeaders())
                .body(e.responseBodyAsByteArray)
        }

    private fun ensureSuccessfulResponse(response: ResponseEntity<ByteArray>) {
        if (!response.statusCode.is2xxSuccessful) {
            throw ExternalApiException()
        }
    }

    private fun isPdf(response: ResponseEntity<ByteArray>, bytes: ByteArray): Boolean {
        val contentType = response.headerValue("content-type")
        return contentType?.contains("pdf", ignoreCase = true) == true ||
            bytes.take(4).toByteArray().decodeToString() == "%PDF"
    }

    private fun isProcessing(response: ResponseEntity<ByteArray>, bytes: ByteArray): Boolean {
        val result = response.headerValue("result")
        if (result == "2") {
            return true
        }

        val bodyText = runCatching { bytes.decodeToString() }.getOrDefault("")
        return bodyText.contains("처리중") || bodyText.contains("\"result\":2") || bodyText.contains("\"result\": 2")
    }

    private fun throwIfApickJsonError(bytes: ByteArray, operation: String) {
        val bodyText = runCatching { bytes.decodeToString() }.getOrDefault("")
        if (!bodyText.trimStart().startsWith("{")) return

        val root = runCatching { objectMapper.readTree(bodyText) }.getOrNull() ?: return
        val error = root.path("result").path("error").asText("")
            .ifBlank { root.path("data").path("error").asText("") }
        val success = root.path("data").path("success").asInt(1)
        val apiSuccess = root.path("api").path("success").takeIf { it.isBoolean }?.asBoolean() ?: true

        if (error.isNotBlank() || success == 0 || !apiSuccess) {
            log.warn(
                "Apick {} failed. error={} success={} apiSuccess={} body={}",
                operation,
                error,
                success,
                apiSuccess,
                bodyText.take(MAX_LOG_BODY_CHARS),
            )
            throw ExternalApiException()
        }
    }

    private fun ResponseEntity<ByteArray>.headerValue(name: String): String? =
        headers[name]?.firstOrNull()
            ?: headers[name.lowercase()]?.firstOrNull()
            ?: headers[name.uppercase()]?.firstOrNull()

    private fun buildAddress(request: AnalysisRequest): String {
        val baseAddress = request.property.roadAddress.ifBlank { request.property.jibunAddress }
        return appendDetailAddress(baseAddress, request.property.detailAddress)
    }

    private fun appendDetailAddress(baseAddress: String, detailAddress: String?): String {
        val base = baseAddress.trim()
        val detail = detailAddress?.trim().orEmpty()
        if (base.isBlank()) return detail
        if (detail.isBlank()) return base

        return if (normalizeAddress(base).contains(normalizeAddress(detail))) {
            base
        } else {
            "$base $detail"
        }
    }

    private fun normalizeAddress(value: String): String =
        value.replace("\\s+".toRegex(), "")

    private fun defaultRegistryType(request: AnalysisRequest): String =
        if (request.property.isApartment || request.property.detailAddress?.isNotBlank() == true) {
            "집합건물"
        } else {
            "건물"
        }

    companion object {
        private const val MAX_DOWNLOAD_ATTEMPTS = 10
        private const val DOWNLOAD_RETRY_DELAY_MILLIS = 5_000L
        private const val MAX_LOG_BODY_CHARS = 1000
    }
}
