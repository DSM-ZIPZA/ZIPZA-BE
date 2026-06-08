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
        @JsonAlias("년", "dealYear")
        val dealYear: String? = null,

        @JsonAlias("월", "dealMonth")
        val dealMonth: String? = null,

        @JsonAlias("일", "dealDay")
        val dealDay: String? = null,

        @JsonAlias("법정동", "umdNm")
        val legalDong: String? = null,

        @JsonAlias("지번", "jibun")
        val jibun: String? = null,

        @JsonAlias("지역코드", "sggCd")
        val regionalCode: String? = null,

        @JsonAlias("아파트", "연립다세대", "오피스텔", "단독다가구", "aptNm", "mhouseNm", "offiNm")
        val buildingName: String? = null,

        @JsonAlias("보증금액", "deposit")
        val depositAmount: String? = null,

        @JsonAlias("월세금액", "monthlyRent")
        val monthlyRent: String? = null,

        @JsonAlias("전용면적", "excluUseAr", "totalFloorAr")
        val exclusiveArea: String? = null,

        @JsonAlias("층", "floor")
        val floor: String? = null,

        @JsonAlias("계약구분", "contractType")
        val contractClassification: String? = null,

        @JsonAlias("계약기간", "contractTerm")
        val contractTerm: String? = null,

        @JsonAlias("종전계약보증금", "preDeposit")
        val previousDeposit: String? = null,

        @JsonAlias("종전계약월세", "preMonthlyRent")
        val previousMonthlyRent: String? = null,

        @JsonAlias("갱신요구권사용", "useRRRight")
        val renewalRightUsage: String? = null,

        @JsonAlias("건축년도", "buildYear")
        val buildYear: String? = null,
    )
}
