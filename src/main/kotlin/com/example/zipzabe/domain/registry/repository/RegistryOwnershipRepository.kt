package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.registry.entity.RegistryOwnership
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistryOwnershipRepository : JpaRepository<RegistryOwnership, UUID> {
    fun findByRegistryRawOrderByRankNumberAsc(registryRaw: RegistryRaw): List<RegistryOwnership>
}
