package com.example.zipzabe.domain.trade.repository

import com.example.zipzabe.domain.property.entity.Property
import com.example.zipzabe.domain.trade.entity.TradeRecord
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TradeRecordRepository : JpaRepository<TradeRecord, UUID> {
    fun findByProperty(property: Property): List<TradeRecord>

    fun findByPropertyOrderByContractDateDesc(property: Property): List<TradeRecord>
}
