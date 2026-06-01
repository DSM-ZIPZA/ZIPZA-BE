package com.example.zipzabe.domain.trade.dto

import com.example.zipzabe.domain.trade.entity.BuildingType
import com.example.zipzabe.domain.trade.entity.ContractClassification
import com.example.zipzabe.domain.trade.entity.ContractType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RentTradeFetchResponse(
    val requestId: UUID,
    val lawdCd: String,
    val dealYmFrom: String,
    val dealYmTo: String,
    val buildingType: BuildingType,
    val fetchedCount: Int,
    val savedCount: Int,
    val records: List<RentTradeRecordResponse>,
)

data class RentTradeListResponse(
    val requestId: UUID,
    val count: Int,
    val records: List<RentTradeRecordResponse>,
)

data class RentTradeRecordResponse(
    val id: UUID?,
    val buildingType: BuildingType,
    val contractType: ContractType,
    @Schema(description = "보증금. 만원 단위")
    val depositAmount: Long,
    @Schema(description = "월세. 만원 단위")
    val monthlyRent: Long?,
    @Schema(description = "전용면적. ㎡ 단위")
    val exclusiveArea: Double,
    val floor: Int,
    val contractDate: LocalDate,
    val contractClassification: ContractClassification?,
    val contractTerm: String?,
    val previousDeposit: Long?,
    val previousMonthlyRent: Long?,
    val fetchedAt: LocalDateTime,
)
