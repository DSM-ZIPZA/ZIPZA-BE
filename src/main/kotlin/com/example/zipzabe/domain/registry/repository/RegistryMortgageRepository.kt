package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.registry.entity.RegistryMortgage
import com.example.zipzabe.domain.registry.entity.RegistryRaw
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistryMortgageRepository : JpaRepository<RegistryMortgage, UUID> {
    fun findByRegistryRawOrderByRankNumberAsc(registryRaw: RegistryRaw): List<RegistryMortgage>
}
