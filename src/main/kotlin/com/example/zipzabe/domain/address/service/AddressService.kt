package com.example.zipzabe.domain.address.service

import com.example.zipzabe.domain.address.dto.AddressDocumentResponse
import com.example.zipzabe.domain.address.dto.AddressResolveResponse
import com.example.zipzabe.domain.address.dto.AddressSearchResponse
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Service
class AddressService(
    @Value("\${kakao.rest-api-key:}") private val kakaoRestApiKey: String,
) {
    private val restClient = RestClient.builder()
        .baseUrl("https://dapi.kakao.com")
        .build()

    fun search(query: String): AddressSearchResponse {
        if (kakaoRestApiKey.isBlank() || query.isBlank()) return AddressSearchResponse(emptyList())
        val body = runCatching {
            restClient.get()
            .uri { builder ->
                builder.path("/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .build()
            }
            .header("Authorization", "KakaoAK $kakaoRestApiKey")
            .retrieve()
            .body(JsonNode::class.java)
        }.getOrNull()

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
                ?: return resolveFromAddressTokens(query)
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
        val body = try {
            restClient.get()
            .uri { builder ->
                builder.path("/v2/local/search/address.json")
                    .queryParam("query", query)
                    .build()
            }
            .header("Authorization", "KakaoAK $kakaoRestApiKey")
            .retrieve()
            .body(JsonNode::class.java)
        } catch (_: RestClientException) {
            null
        }

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

    private fun resolveFromAddressTokens(query: String): AddressResolveResponse {
        val tokens = query.split(" ").filter { it.isNotBlank() }
        val city = tokens.getOrNull(0).orEmpty()
        val district = tokens.getOrNull(1).orEmpty()
        val neighborhood = tokens
            .getOrNull(2)
            ?.takeIf { LEGAL_DONG_SUFFIXES.any(it::endsWith) }
            .orEmpty()
        val lawdCd = resolveLawdCd(city, district)
        return AddressResolveResponse(
            roadAddress = query,
            jibunAddress = query,
            administrativeCode = lawdCd?.let { "${it}00000" }.orEmpty(),
            city = city,
            district = district,
            neighborhood = neighborhood,
            latitude = 0.0,
            longitude = 0.0,
        )
    }

    private fun resolveLawdCd(city: String, district: String): String? {
        val normalizedCity = city.removeSuffix("광역시").removeSuffix("특별시")
        return LAWD_CD_BY_CITY_DISTRICT[normalizedCity to district]
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

    companion object {
        private val LEGAL_DONG_SUFFIXES = listOf("동", "읍", "면", "가", "리")

        private val LAWD_CD_BY_CITY_DISTRICT = mapOf(
            ("대전" to "동구") to "30110",
            ("대전" to "중구") to "30140",
            ("대전" to "서구") to "30170",
            ("대전" to "유성구") to "30200",
            ("대전" to "대덕구") to "30230",
            ("서울" to "종로구") to "11110",
            ("서울" to "중구") to "11140",
            ("서울" to "용산구") to "11170",
            ("서울" to "성동구") to "11200",
            ("서울" to "광진구") to "11215",
            ("서울" to "동대문구") to "11230",
            ("서울" to "중랑구") to "11260",
            ("서울" to "성북구") to "11290",
            ("서울" to "강북구") to "11305",
            ("서울" to "도봉구") to "11320",
            ("서울" to "노원구") to "11350",
            ("서울" to "은평구") to "11380",
            ("서울" to "서대문구") to "11410",
            ("서울" to "마포구") to "11440",
            ("서울" to "양천구") to "11470",
            ("서울" to "강서구") to "11500",
            ("서울" to "구로구") to "11530",
            ("서울" to "금천구") to "11545",
            ("서울" to "영등포구") to "11560",
            ("서울" to "동작구") to "11590",
            ("서울" to "관악구") to "11620",
            ("서울" to "서초구") to "11650",
            ("서울" to "강남구") to "11680",
            ("서울" to "송파구") to "11710",
            ("서울" to "강동구") to "11740",
        )
    }
}
