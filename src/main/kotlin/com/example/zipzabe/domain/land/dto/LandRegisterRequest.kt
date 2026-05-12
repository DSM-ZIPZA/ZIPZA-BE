package com.example.zipzabe.domain.land.dto

data class LandRegisterRequest(
    val address: String,
    val landType: String,
    val bonbun: String,
    val bubun: String? = null,
)
