package com.example.zipzabe.domain.registry.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class RegistryOcrResponse(
    val registryRawId: UUID,
    val uniqueNumber: String,
    val sourceHash: String,
    val pageCount: Int,
    val extractedTextLength: Int,
    val titleCount: Int,
    val ownershipCount: Int,
    val restrictionCount: Int,
    val mortgageCount: Int,
)

data class RegistryApickOcrRequest(
    val address: String? = null,
    @JsonProperty("unique_num")
    val uniqueNum: String? = null,
    val type: String? = null,
)

data class ApickIrosIssueResponse(
    val data: ApickIrosIssueData? = null,
    val api: ApickApiMetadata? = null,
)

data class ApickIrosIssueData(
    @JsonProperty("ic_id")
    val icId: Long? = null,
    val success: Int? = null,
)

data class ApickApiMetadata(
    val success: Boolean? = null,
    val cost: Int? = null,
    val ms: Int? = null,
    @JsonProperty("pl_id")
    val plId: Long? = null,
)
