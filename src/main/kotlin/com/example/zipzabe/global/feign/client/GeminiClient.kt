package com.example.zipzabe.global.feign.client

import com.example.zipzabe.global.feign.dto.gemini.GeminiRequest
import com.example.zipzabe.global.feign.dto.gemini.GeminiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "gemini",
    url = "\${gemini.url}"
)
interface GeminiClient {

    @PostMapping("/v1beta/models/{model}:generateContent")
    fun generateContent(
        @PathVariable("model") model: String,
        @RequestParam("key") apiKey: String,
        @RequestBody request: GeminiRequest
    ): GeminiResponse
}
