package com.example.zipzabe.domain.property.repository

import com.example.zipzabe.domain.property.entity.Property
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PropertyRepository : JpaRepository<Property, UUID>
