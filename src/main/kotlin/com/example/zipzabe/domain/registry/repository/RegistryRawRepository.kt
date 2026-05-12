package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistryRawRepository : JpaRepository<RegistryRaw, UUID> {
    fun findTopByRequestOrderByFetchedAtDesc(request: AnalysisRequest): RegistryRaw?
}
