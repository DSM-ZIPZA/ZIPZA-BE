package com.example.zipzabe.domain.report.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.report.entity.ManualCheckItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ManualCheckItemRepository : JpaRepository<ManualCheckItem, UUID> {
    fun findByRequestOrderByCreatedAtAsc(request: AnalysisRequest): List<ManualCheckItem>
    fun deleteAllByRequest(request: AnalysisRequest)
}
