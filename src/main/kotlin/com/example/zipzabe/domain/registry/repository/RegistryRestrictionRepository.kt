package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.registry.entity.RegistryRaw
import com.example.zipzabe.domain.registry.entity.RegistryRestriction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface
RegistryRestrictionRepository : JpaRepository<RegistryRestriction, UUID> {
    fun findByRegistryRawOrderByRankNumberAsc(registryRaw: RegistryRaw): List<RegistryRestriction>
}
