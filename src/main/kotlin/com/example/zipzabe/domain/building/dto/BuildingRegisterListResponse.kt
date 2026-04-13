package com.example.zipzabe.domain.building.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class BuildingRegisterListResponse(
    val data: Data,
    val api: Api,
) {
    data class Data(
        @JsonProperty("ic_id") val icId: Int,
        val result: Int,
        val data: List<BuildingItem>,
        val success: Int,
    )

    data class BuildingItem(
        @JsonProperty("건축물명칭") val buildingName: String,
        @JsonProperty("동명칭") val dongName: String,
        @JsonProperty("호명칭") val hoName: String,
        @JsonProperty("연면적") val floorArea: String,
    )

    data class Api(
        val success: Boolean,
        val cost: Int,
        val ms: Int,
        @JsonProperty("pl_id") val plId: Int,
    )
}
