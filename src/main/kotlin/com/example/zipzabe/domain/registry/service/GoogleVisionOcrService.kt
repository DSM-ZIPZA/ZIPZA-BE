package com.example.zipzabe.domain.registry.service

import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.util.Base64

@Service
class GoogleVisionOcrService(
    @Value("\${google.vision.api-key:}")
    private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(GoogleVisionOcrService::class.java)
    private val restClient = RestClient.create()

    fun extractText(pageImages: List<ByteArray>): String {
        if (pageImages.isEmpty()) {
            return ""
        }
        if (apiKey.isBlank()) {
            throw ExternalApiBadRequestException()
        }

        return pageImages.mapIndexed { index, pageImage ->
            extractPageText(index, pageImage)
        }.joinToString("\n\n")
    }

    private fun extractPageText(pageIndex: Int, pageImage: ByteArray): String {
        val request = VisionAnnotateRequest(
            requests = listOf(
                VisionImageRequest(
                    image = VisionImage(content = Base64.getEncoder().encodeToString(pageImage)),
                    features = listOf(VisionFeature(type = "DOCUMENT_TEXT_DETECTION")),
                    imageContext = VisionImageContext(languageHints = listOf("ko", "en")),
                ),
            ),
        )

        log.info(
            "Requesting Google Vision OCR page. page={} imageBytes={}",
            pageIndex + 1,
            pageImage.size,
        )
        val response = try {
            restClient.post()
                .uri("https://vision.googleapis.com/v1/images:annotate?key={apiKey}", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VisionAnnotateResponse::class.java)
        } catch (e: RestClientResponseException) {
            log.warn(
                "Google Vision OCR failed. status={} body={}",
                e.statusCode.value(),
                e.responseBodyAsString.take(MAX_LOG_BODY_CHARS),
                e,
            )
            throw ExternalApiException()
        } catch (e: RestClientException) {
            log.warn("Google Vision OCR request failed. message={}", e.message, e)
            throw ExternalApiException()
        } ?: throw ExternalApiException()

        val pageResponse = response.responses.firstOrNull() ?: throw ExternalApiException()
        pageResponse.error?.let {
            log.warn("Google Vision OCR page failed. page={} message={}", pageIndex + 1, it.message)
            throw ExternalApiException()
        }
        val text = pageResponse.fullTextAnnotation?.text
            ?.takeIf { it.isNotBlank() }
            ?: pageResponse.textAnnotations.firstOrNull()?.description.orEmpty()
        return "--- page ${pageIndex + 1} ---\n$text"
    }

    companion object {
        private const val MAX_LOG_BODY_CHARS = 1000
    }
}

private data class VisionAnnotateRequest(
    val requests: List<VisionImageRequest>,
)

private data class VisionImageRequest(
    val image: VisionImage,
    val features: List<VisionFeature>,
    val imageContext: VisionImageContext,
)

private data class VisionImage(
    val content: String,
)

private data class VisionFeature(
    val type: String,
)

private data class VisionImageContext(
    val languageHints: List<String>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class VisionAnnotateResponse(
    val responses: List<VisionImageResponse> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class VisionImageResponse(
    val fullTextAnnotation: VisionFullTextAnnotation? = null,
    val textAnnotations: List<VisionTextAnnotation> = emptyList(),
    val error: VisionError? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class VisionFullTextAnnotation(
    val text: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class VisionTextAnnotation(
    val description: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class VisionError(
    val message: String = "",
)
