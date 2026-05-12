package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AnalysisRequestRepository : JpaRepository<AnalysisRequest, UUID>
