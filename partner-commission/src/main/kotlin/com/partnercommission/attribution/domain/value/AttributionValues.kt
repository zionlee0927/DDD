package com.partnercommission.attribution.domain.value

enum class AttributionStatus {
    ATTRIBUTED,
    REVOKED,
    UNATTRIBUTED,
}

enum class AttributionStrategy {
    LAST_CLICK,
    FIRST_CLICK,
}

enum class EvidenceType {
    CLICK,
    REFERRAL_CODE,
}

data class AttributionEvidence(
    val type: EvidenceType,
    val referenceId: String,
) {
    init {
        require(referenceId.isNotBlank()) { "귀속 증거 referenceId는 비어있을 수 없다" }
    }
}
