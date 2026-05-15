package com.example.zipzabe.domain.analysis.repository

import com.example.zipzabe.domain.analysis.entity.AnalysisRequest
import com.example.zipzabe.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AnalysisRequestRepository : JpaRepository<AnalysisRequest, UUID> {
    fun findByUserOrderByRequestedAtDesc(user: User): List<AnalysisRequest>

    fun findByIdAndUser(id: UUID, user: User): AnalysisRequest?
}
