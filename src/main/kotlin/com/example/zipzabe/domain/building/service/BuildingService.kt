package com.example.zipzabe.domain.building.service

import com.example.zipzabe.domain.building.dto.BuildingRegisterListResponse
import com.example.zipzabe.domain.building.dto.BuildingRegisterRequest
import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.example.zipzabe.global.error.exception.ExternalApiNotFoundException
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
class BuildingService(
    @Value("\${apick.url}") apickUrl: String,
    @Value("\${apick.auth-key}") private val authKey: String,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(BuildingService::class.java)
    private val restClient = RestClient.builder()
        .baseUrl(apickUrl.trimEnd('/'))
        .build()

    fun getBuildingRegisterPdf(request: BuildingRegisterRequest): ByteArray {
        return runCatching { requestBuildingRegisterPdf(request) }
            .recoverCatching { original ->
                val resolvedRequest = resolveRegisterRequestFromList(request) ?: throw original
                log.info(
                    "Retrying building register with Apick list candidate. address={} originalBName={} originalDong={} originalHo={} resolvedBName={} resolvedDong={} resolvedHo={}",
                    request.address,
                    request.bName,
                    request.dong,
                    request.ho,
                    resolvedRequest.bName,
                    resolvedRequest.dong,
                    resolvedRequest.ho,
                )
                requestBuildingRegisterPdf(resolvedRequest)
            }
            .getOrThrow()
    }

    private fun requestBuildingRegisterPdf(request: BuildingRegisterRequest): ByteArray {
        repeat(MAX_REGISTER_ATTEMPTS) { attempt ->
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("address", request.address)
                add("b_name", request.bName)
                add("dong", request.dong)
                add("ho", request.ho)
            }
            log.info(
                "Requesting Apick building register. address={} bName={} dong={} ho={} attempt={}",
                request.address,
                request.bName,
                request.dong,
                request.ho,
                attempt + 1,
            )
            val response = postMultipart("/rest/building_register", body)
            val bytes = response.body ?: ByteArray(0)

            if (isPdf(response, bytes)) {
                log.info(
                    "Apick building register PDF downloaded. address={} bName={} dong={} ho={} attempt={} bytes={}",
                    request.address,
                    request.bName,
                    request.dong,
                    request.ho,
                    attempt + 1,
                    bytes.size,
                )
                return bytes
            }

            if (isProcessing(response, bytes) && attempt < MAX_REGISTER_ATTEMPTS - 1) {
                Thread.sleep(REGISTER_RETRY_DELAY_MILLIS)
                return@repeat
            }

            ensureSuccessfulPdfResponse(response)
            throwIfApickJsonError(bytes, "buildingRegister")
            throw ExternalApiException()
        }

        throw ExternalApiException()
    }

    fun getBuildingRegisterList(address: String): BuildingRegisterListResponse {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("address", address)
        }
        val response = postMultipart("/rest/get_building_register_list", body)
        ensureSuccessfulPdfResponse(response)
        val bytes = response.body ?: ByteArray(0)
        throwIfApickJsonError(bytes, "buildingRegisterList")
        return runCatching {
            objectMapper.readValue(bytes, BuildingRegisterListResponse::class.java)
        }.getOrElse { e ->
            log.warn(
                "Failed to parse Apick building register list response. address={} body={}",
                address,
                bytes.decodeToString().take(MAX_LOG_BODY_CHARS),
                e,
            )
            throw ExternalApiException()
        }
    }

    private fun resolveRegisterRequestFromList(request: BuildingRegisterRequest): BuildingRegisterRequest? {
        val items = runCatching { getBuildingRegisterList(request.address).data.data }
            .getOrElse { e ->
                log.warn(
                    "Failed to resolve building register candidates. address={} bName={} dong={} ho={}",
                    request.address,
                    request.bName,
                    request.dong,
                    request.ho,
                    e,
                )
                return null
            }
        if (items.isEmpty()) return null

        val selected = items.maxByOrNull { scoreCandidate(it, request) } ?: return null
        val selectedScore = scoreCandidate(selected, request)
        if (selectedScore <= 0) {
            log.warn(
                "No exact building register candidate found; using first Apick candidate. address={} requestedBName={} requestedDong={} requestedHo={} candidateCount={}",
                request.address,
                request.bName,
                request.dong,
                request.ho,
                items.size,
            )
        }

        return BuildingRegisterRequest(
            address = request.address,
            bName = selected.buildingName,
            dong = selected.dongName,
            ho = selected.hoName,
        )
    }

    private fun scoreCandidate(
        item: BuildingRegisterListResponse.BuildingItem,
        request: BuildingRegisterRequest,
    ): Int {
        val requestedBuilding = normalize(request.bName)
        val candidateBuilding = normalize(item.buildingName)
        val requestedDong = normalizeUnit(request.dong)
        val candidateDong = normalizeUnit(item.dongName)
        val requestedHo = normalizeUnit(request.ho)
        val candidateHo = normalizeUnit(item.hoName)

        var score = 0
        if (requestedHo.isNotBlank() && candidateHo == requestedHo) score += 100
        if (requestedDong.isNotBlank() && candidateDong == requestedDong) score += 60
        if (
            requestedBuilding.isNotBlank() &&
            (candidateBuilding == requestedBuilding ||
                candidateBuilding.contains(requestedBuilding) ||
                requestedBuilding.contains(candidateBuilding))
        ) {
            score += 30
        }
        if (requestedDong.isNotBlank() && candidateDong.contains(requestedDong)) score += 20
        if (requestedHo.isNotBlank() && candidateHo.endsWith(requestedHo)) score += 10
        return score
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

    private fun ensureSuccessfulPdfResponse(response: ResponseEntity<ByteArray>) {
        when (response.statusCode.value()) {
            in 200..299 -> return
            400 -> throw ExternalApiBadRequestException()
            404 -> throw ExternalApiNotFoundException()
            else -> throw ExternalApiException()
        }
    }

    private fun isPdf(response: ResponseEntity<ByteArray>, bytes: ByteArray): Boolean {
        val contentType = response.headerValue("content-type")
        return contentType?.contains("pdf", ignoreCase = true) == true ||
            bytes.take(4).toByteArray().decodeToString() == "%PDF"
    }

    private fun isProcessing(response: ResponseEntity<ByteArray>, bytes: ByteArray): Boolean {
        val result = response.headerValue("result")
        if (result == "2") return true

        val bodyText = runCatching { bytes.decodeToString() }.getOrDefault("")
        return bodyText.contains("처리중") ||
            bodyText.contains("\"result\":2") ||
            bodyText.contains("\"result\": 2")
    }

    private fun throwIfApickJsonError(bytes: ByteArray, operation: String) {
        val bodyText = runCatching { bytes.decodeToString() }.getOrDefault("")
        if (!bodyText.trimStart().startsWith("{")) return

        val root = runCatching { objectMapper.readTree(bodyText) }.getOrNull() ?: return
        val error = root.path("result").path("error").asText("")
            .ifBlank { root.path("data").path("error").asText("") }
        val success = root.path("data").path("success").asInt(1)
        val result = root.path("data").path("result").asInt(1)

        if (error.isNotBlank() || success == 0 || result == 2 || result == 3) {
            log.warn(
                "Apick {} failed. error={} result={} success={} body={}",
                operation,
                error,
                result,
                success,
                bodyText.take(MAX_LOG_BODY_CHARS),
            )
            throw ExternalApiException()
        }
    }

    private fun ResponseEntity<ByteArray>.headerValue(name: String): String? =
        headers[name]?.firstOrNull()
            ?: headers[name.lowercase()]?.firstOrNull()
            ?: headers[name.uppercase()]?.firstOrNull()

    private fun normalize(value: String): String =
        value
            .trim()
            .replace("\\s+".toRegex(), "")

    private fun normalizeUnit(value: String): String =
        normalize(value)
            .removeSuffix("동")
            .removeSuffix("호")

    companion object {
        private const val MAX_REGISTER_ATTEMPTS = 10
        private const val REGISTER_RETRY_DELAY_MILLIS = 5_000L
        private const val MAX_LOG_BODY_CHARS = 1000
    }
}
