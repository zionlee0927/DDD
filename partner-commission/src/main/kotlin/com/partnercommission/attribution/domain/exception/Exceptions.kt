package com.partnercommission.attribution.domain.exception

import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.TenantId

sealed class AttributionException(message: String) : RuntimeException(message)

class DuplicateConversionException(externalId: String) :
    AttributionException("이미 처리된 전환 이벤트: $externalId")

class TenantNotFoundException(tenantId: TenantId) :
    AttributionException("Tenant not found: ${tenantId.value}")

class EvidenceRequiredException :
    AttributionException("증거(clickId 또는 referralCode)가 필요합니다")

class UnsupportedEvidenceTypeException(type: EvidenceType) :
    AttributionException("지원하지 않는 증거 타입: $type")
