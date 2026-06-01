package com.example.zipzabe.domain.address.dto

data class AddressSearchResponse(
    val documents: List<AddressDocumentResponse>,
)

data class AddressDocumentResponse(
    val roadAddress: String,
    val jibunAddress: String,
    val latitude: Double,
    val longitude: Double,
)

data class AddressResolveResponse(
    val roadAddress: String,
    val jibunAddress: String,
    val detailAddress: String? = null,
    val buildingManagementNumber: String = "",
    val postalCode: String = "",
    val administrativeCode: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val buildingName: String? = null,
    val isApartment: Boolean = false,
    val latitude: Double,
    val longitude: Double,
)
