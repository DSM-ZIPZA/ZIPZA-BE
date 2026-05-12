package com.example.zipzabe.global.feign.dto.gemini

data class GeminiRequest(
    val contents: List<Content>
) {
    data class Content(val parts: List<Part>)
    data class Part(val text: String)

    companion object {
        fun of(prompt: String) = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(prompt))))
        )
    }
}
