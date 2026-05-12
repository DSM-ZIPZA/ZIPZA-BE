package com.example.zipzabe.domain.trade.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
data class MolitRentApiResponse(
    @JsonProperty("header")
    val header: Header? = null,

    @JsonProperty("body")
    val body: Body? = null,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Header(
        @JsonProperty("resultCode")
        val resultCode: String? = null,

        @JsonProperty("resultMsg")
        val resultMsg: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Body(
        @JsonProperty("items")
        val items: Items? = null,

        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        val directItems: List<Item> = emptyList(),

        @JsonProperty("numOfRows")
        val numOfRows: Int? = null,

        @JsonProperty("pageNo")
        val pageNo: Int? = null,

        @JsonProperty("totalCount")
        val totalCount: Int? = null,
    ) {
        fun allItems(): List<Item> = items?.items.orEmpty() + directItems
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Items(
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        val items: List<Item> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Item(
        @JsonProperty("년")
        val dealYear: String? = null,

        @JsonProperty("월")
        val dealMonth: String? = null,

        @JsonProperty("일")
        val dealDay: String? = null,

        @JsonProperty("법정동")
        val legalDong: String? = null,

        @JsonProperty("지번")
        val jibun: String? = null,

        @JsonProperty("지역코드")
        val regionalCode: String? = null,

        @JsonAlias("아파트", "연립다세대", "오피스텔", "단독다가구")
        val buildingName: String? = null,

        @JsonProperty("보증금액")
        val depositAmount: String? = null,

        @JsonProperty("월세금액")
        val monthlyRent: String? = null,

        @JsonProperty("전용면적")
        val exclusiveArea: String? = null,

        @JsonProperty("층")
        val floor: String? = null,

        @JsonProperty("계약구분")
        val contractClassification: String? = null,

        @JsonProperty("계약기간")
        val contractTerm: String? = null,

        @JsonProperty("종전계약보증금")
        val previousDeposit: String? = null,

        @JsonProperty("종전계약월세")
        val previousMonthlyRent: String? = null,

        @JsonProperty("갱신요구권사용")
        val renewalRightUsage: String? = null,

        @JsonProperty("건축년도")
        val buildYear: String? = null,
    )
}
