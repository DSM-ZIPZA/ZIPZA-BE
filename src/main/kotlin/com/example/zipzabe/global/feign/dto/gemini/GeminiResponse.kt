package com.example.zipzabe.global.feign.dto.gemini

data class GeminiResponse(
    val candidates: List<Candidate>
) {
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)

    fun extractText(): String =
        candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
}
