package com.example.zipzabe.domain.analysis.service

import com.example.zipzabe.global.feign.client.GeminiClient
import com.example.zipzabe.global.feign.dto.gemini.GeminiRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GeminiSummaryService(
    private val geminiClient: GeminiClient,
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.model}") private val model: String
) {

    fun summarize(prompt: String): String {
        val response = geminiClient.generateContent(model, apiKey, GeminiRequest.of(prompt))
        return response.extractText()
    }
}
