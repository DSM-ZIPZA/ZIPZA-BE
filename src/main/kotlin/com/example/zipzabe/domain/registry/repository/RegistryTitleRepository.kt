package com.example.zipzabe.domain.registry.repository

import com.example.zipzabe.domain.registry.entity.RegistryRaw
import com.example.zipzabe.domain.registry.entity.RegistryTitle
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistryTitleRepository : JpaRepository<RegistryTitle, UUID> {
    fun findByRegistryRaw(registryRaw: RegistryRaw): List<RegistryTitle>
}
