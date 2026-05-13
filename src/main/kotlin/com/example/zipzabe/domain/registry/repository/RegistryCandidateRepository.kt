package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.registry.entity.RegistryCandidate
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistryCandidateRepository : JpaRepository<RegistryCandidate, UUID>
