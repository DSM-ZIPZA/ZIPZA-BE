package com.example.zipzabe.domain.registry.service

import com.example.zipzabe.global.error.exception.ExternalApiBadRequestException
import com.example.zipzabe.global.error.exception.ExternalApiException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.Base64

@Service
class GoogleVisionOcrService(
    @Value("\${google.vision.api-key:}")
    private val apiKey: String,
) {
    private val restClient = RestClient.create()

    fun extractText(pageImages: List<ByteArray>): String {
        if (pageImages.isEmpty()) {
            return ""
        }
        if (apiKey.isBlank()) {
            throw ExternalApiBadRequestException()
        }

        val request = VisionAnnotateRequest(
            requests = pageImages.map { pageImage ->
                VisionImageRequest(
                    image = VisionImage(
                        content = Base64.getEncoder().encodeToString(pageImage),
                    ),
                    features = listOf(VisionFeature(type = "DOCUMENT_TEXT_DETECTION")),
                    imageContext = VisionImageContext(languageHints = listOf("ko", "en")),
                )
            },
        )

        val response = try {
            restClient.post()
                .uri("https://vision.googleapis.com/v1/images:annotate?key={apiKey}", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(VisionAnnotateResponse::class.java)
        } catch (e: RestClientException) {
            throw ExternalApiException()
        } ?: throw ExternalApiException()

        return response.responses.mapIndexed { index, pageResponse ->
            pageResponse.error?.let { throw ExternalApiException() }
            val text = pageResponse.fullTextAnnotation?.text
                ?.takeIf { it.isNotBlank() }
                ?: pageResponse.textAnnotations.firstOrNull()?.description.orEmpty()
            "--- page ${index + 1} ---\n$text"
        }.joinToString("\n\n")
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
