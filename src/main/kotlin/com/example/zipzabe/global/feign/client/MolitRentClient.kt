package com.example.zipzabe.global.feign.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "molitRent",
    url = "\${molit.rent.url}",
)
interface MolitRentClient {

    @GetMapping("/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent")
    fun getApartmentRent(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("LAWD_CD") lawdCd: String,
        @RequestParam("DEAL_YMD") dealYmd: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
    ): String

    @GetMapping("/1613000/RTMSDataSvcRHRent/getRTMSDataSvcRHRent")
    fun getRowHouseRent(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("LAWD_CD") lawdCd: String,
        @RequestParam("DEAL_YMD") dealYmd: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
    ): String

    @GetMapping("/1613000/RTMSDataSvcOffiRent/getRTMSDataSvcOffiRent")
    fun getOfficetelRent(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("LAWD_CD") lawdCd: String,
        @RequestParam("DEAL_YMD") dealYmd: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
    ): String

    @GetMapping("/1613000/RTMSDataSvcSHRent/getRTMSDataSvcSHRent")
    fun getDetachedHouseRent(
        @RequestParam("serviceKey") serviceKey: String,
        @RequestParam("LAWD_CD") lawdCd: String,
        @RequestParam("DEAL_YMD") dealYmd: String,
        @RequestParam("pageNo") pageNo: Int,
        @RequestParam("numOfRows") numOfRows: Int,
    ): String
}
