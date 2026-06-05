package com.example.zipzabe.global.apick

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

@Component
class ApickMultipartClient(
    @Value("\${apick.url}") apickUrl: String,
    @Value("\${apick.auth-key}") private val authKey: String,
) {
    private val baseUrl = apickUrl.trimEnd('/')
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    fun post(path: String, fields: Map<String, String>): ResponseEntity<ByteArray> {
        val boundary = "zipza-${UUID.randomUUID()}"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .timeout(Duration.ofSeconds(60))
            .header("CL_AUTH_KEY", authKey)
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(boundary, fields)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        val headers = HttpHeaders()
        response.headers().map().forEach { (name, values) -> headers.addAll(name, values) }
        return ResponseEntity
            .status(response.statusCode())
            .headers(headers)
            .body(response.body())
    }

    private fun buildMultipartBody(boundary: String, fields: Map<String, String>): ByteArray {
        val builder = StringBuilder()
        fields.forEach { (name, value) ->
            builder.append("--").append(boundary).append(CRLF)
            builder.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(CRLF)
            builder.append(CRLF)
            builder.append(value).append(CRLF)
        }
        builder.append("--").append(boundary).append("--").append(CRLF)
        return builder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        private const val CRLF = "\r\n"
    }
}
