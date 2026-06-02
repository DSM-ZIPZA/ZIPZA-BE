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
        val addressDocument = searchAddress(query) ?: run {
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

        return AddressResolveResponse(
            roadAddress = addressDocument.roadAddress.ifBlank { addressDocument.jibunAddress },
            jibunAddress = addressDocument.jibunAddress.ifBlank { addressDocument.roadAddress },
            administrativeCode = addressDocument.administrativeCode,
            city = addressDocument.city,
            district = addressDocument.district,
            neighborhood = addressDocument.neighborhood,
            latitude = addressDocument.latitude,
            longitude = addressDocument.longitude,
        )
    }

    private fun searchAddress(query: String): ResolvedAddressDocument? {
        if (kakaoRestApiKey.isBlank() || query.isBlank()) return null
        val body = restClient.get()
            .uri { builder ->
                builder.path("/v2/local/search/address.json")
                    .queryParam("query", query)
                    .build()
            }
            .header("Authorization", "KakaoAK $kakaoRestApiKey")
            .retrieve()
            .body(JsonNode::class.java)

        return body?.path("documents")?.firstOrNull()?.let { node ->
            val road = node.path("road_address")
            val address = node.path("address")
            val roadAddress = road.path("address_name").asText("")
            val jibunAddress = address.path("address_name").asText("")
            val lat = node.path("y").asText("").toDoubleOrNull()
                ?: address.path("y").asText("").toDoubleOrNull()
                ?: road.path("y").asText("").toDoubleOrNull()
                ?: 0.0
            val lng = node.path("x").asText("").toDoubleOrNull()
                ?: address.path("x").asText("").toDoubleOrNull()
                ?: road.path("x").asText("").toDoubleOrNull()
                ?: 0.0

            ResolvedAddressDocument(
                roadAddress = roadAddress,
                jibunAddress = jibunAddress,
                administrativeCode = address.path("b_code").asText(""),
                city = address.path("region_1depth_name").asText(""),
                district = address.path("region_2depth_name").asText(""),
                neighborhood = address.path("region_3depth_name").asText(""),
                latitude = lat,
                longitude = lng,
            )
        }
    }

    private data class ResolvedAddressDocument(
        val roadAddress: String,
        val jibunAddress: String,
        val administrativeCode: String,
        val city: String,
        val district: String,
        val neighborhood: String,
        val latitude: Double,
        val longitude: Double,
    )
}
