package com.example.zipzabe.domain.address.controller

import com.example.zipzabe.domain.address.dto.AddressResolveResponse
import com.example.zipzabe.domain.address.dto.AddressSearchResponse
import com.example.zipzabe.domain.address.service.AddressService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "주소", description = "주소 검색과 분석 요청용 주소 보강 API")
@RestController
@RequestMapping("/api/address")
class AddressController(
    private val addressService: AddressService,
) {
    @Operation(summary = "주소 검색", description = "Kakao 주소 검색을 백엔드에서 프록시합니다.")
    @GetMapping("/search")
    fun search(@RequestParam query: String): AddressSearchResponse =
        addressService.search(query)

    @Operation(summary = "주소 보강", description = "분석 요청 생성에 필요한 주소 메타데이터 기본값을 제공합니다.")
    @GetMapping("/resolve")
    fun resolve(@RequestParam query: String): AddressResolveResponse =
        addressService.resolve(query)
}
