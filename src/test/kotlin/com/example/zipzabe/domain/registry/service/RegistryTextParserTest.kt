package com.example.zipzabe.domain.registry.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegistryTextParserTest {

    private val parser = RegistryTextParser()

    @Test
    fun `parses spaced registry section markers and current rights`() {
        val text = """
            등기사항전부증명서(말소사항 포함)
            - 집합건물 -
            고유번호 1358-2019-013681
            [집합건물] 경기도 수원시 권선구 호매실동 1420 호매실금호어울림에듀포레 제102동 제18층 제1803호
            【  표  제  부  】 ( 전유부분의 건물의 표시 )
            표시번호 접수 건물번호 건물내역
            1 2019년10월14일 제18층 제1803호 철근콘크리트구조
            72.9161㎡
            【  갑      구  】 ( 소유권에 관한 사항 )
            순위번호 등 기 목 적 접 수 등 기 원 인 권리자 및 기타사항
            1 소유권보존 2019년10월14일 소유자 한국자산신탁주식회사 110111-2196304
            2 소유권이전 2019년11월21일 2017년7월21일 소유자 강봉구 770615-*******
            【  을      구  】 ( 소유권 이외의 권리에 관한 사항 )
            순위번호 등 기 목 적 접수 등기원인 권리자 및 기타사항
            1 근저당권설정 2019년11월21일 2019년9월18일 채권최고액 금240,000,000원
            채무자 강봉구
            근저당권자 농협은행주식회사 110111-4809385
        """.trimIndent()

        val parsed = parser.parse(
            text = text,
            fallbackAddress = "fallback",
            fallbackBuildingName = "fallbackBuilding",
        )

        assertEquals("1358-2019-013681", parsed.uniqueNumber)
        assertEquals("집합건물", parsed.title.realEstateType)
        assertTrue(parsed.title.locationAddress.contains("호매실동 1420"))
        assertEquals("호매실금호어울림에듀포레", parsed.title.buildingName)
        assertEquals("제18층제1803호", parsed.title.floorInfo)
        assertEquals(72.9161, parsed.title.exclusiveArea)
        assertEquals(2, parsed.ownerships.size)
        assertEquals("강봉구", parsed.ownerships.last().ownerName)
        assertTrue(parsed.ownerships.last().isCurrent)
        assertEquals(1, parsed.mortgages.size)
        assertEquals(24_000L, parsed.mortgages.first().claimAmount)
        assertEquals("농협은행주식회사", parsed.mortgages.first().creditorName)
    }
}
