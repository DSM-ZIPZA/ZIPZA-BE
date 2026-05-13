package com.example.zipzabe.domain.registry.dto

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
