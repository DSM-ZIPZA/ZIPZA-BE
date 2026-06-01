package com.example.zipzabe.domain.address.service

import com.example.zipzabe.domain.address.dto.AddressDocumentResponse
import com.example.zipzabe.domain.address.dto.AddressResolveResponse
import com.example.zipzabe.domain.address.dto.AddressSearchResponse
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AddressService(
    @Value("\${kakao.rest-api-key:}") private val kakaoRestApiKey: String,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .build()

    fun search(query: String): AddressSearchResponse {
        if (kakaoRestApiKey.isBlank() || query.isBlank()) return AddressSearchResponse(emptyList())
        val body = restClient.get()
            .uri { builder ->
                builder.path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .build()
            }
            .header("Authorization", "KakaoAK $kakaoRestApiKey")
            .retrieve()
            .body(JsonNode::class.java)

        val documents = body?.path("documents")
            ?.mapNotNull { node ->
                val road = node.path("road_address_name").asText("")
                val jibun = node.path("address_name").asText("")
                val lat = node.path("y").asText("").toDoubleOrNull()
                val lng = node.path("x").asText("").toDoubleOrNull()
                if (lat == null || lng == null) null
                else AddressDocumentResponse(
                    roadAddress = road,
                    jibunAddress = jibun,
                    latitude = lat,
                    longitude = lng,
                )
            }
            .orEmpty()
        return AddressSearchResponse(documents)
    }

    fun resolve(query: String): AddressResolveResponse {
        val first = search(query).documents.firstOrNull()
            ?: AddressDocumentResponse(query, query, 0.0, 0.0)
        val address = first.roadAddress.ifBlank { first.jibunAddress }
        val tokens = address.split(" ").filter { it.isNotBlank() }
        return AddressResolveResponse(
            roadAddress = first.roadAddress.ifBlank { first.jibunAddress },
            jibunAddress = first.jibunAddress.ifBlank { first.roadAddress },
            city = tokens.getOrNull(0).orEmpty(),
            district = tokens.getOrNull(1).orEmpty(),
            neighborhood = tokens.getOrNull(2).orEmpty(),
            latitude = first.latitude,
            longitude = first.longitude,
        )
    }
}
