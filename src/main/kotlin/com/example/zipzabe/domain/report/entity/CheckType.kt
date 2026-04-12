package com.example.zipzabe.domain.report.entity

enum class CheckType {
    TENANT_REGISTRATION,   // 전입신고
    FIXED_DATE,            // 확정일자
    BUILDING_REGISTRY,     // 건축물대장 확인
    SITE_INSPECTION,       // 현장 방문
    INSURANCE_CHECK,       // 전세보증보험 가입 확인
    REGISTRY_MONITORING    // 등기부 모니터링
}
