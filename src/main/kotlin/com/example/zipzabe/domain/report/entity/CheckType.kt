package com.example.zipzabe.domain.report.entity

enum class CheckType {
    TENANT_REGISTRATION,            // 전입신고
    FIXED_DATE,                     // 확정일자
    BUILDING_REGISTRY,              // 건축물대장 확인
    SITE_INSPECTION,                // 현장 방문
    INSURANCE_CHECK,                // 전세보증보험 가입 확인
    REGISTRY_MONITORING,            // 등기부 모니터링
    RESIDENT_REGISTRATION_CONFIRM,  // 전입세대확인서 발급/열람
    UNPAID_NATIONAL_TAX_INQUIRY,    // 미납국세 열람
    WAGE_CLAIM_PRIORITY,            // 임금채권 우선변제 위험
}
