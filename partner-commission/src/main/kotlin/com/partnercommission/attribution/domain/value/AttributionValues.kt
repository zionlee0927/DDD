package com.partnercommission.attribution.domain.value

import com.partnercommission.shared.domain.value.PartnerId
import java.time.Duration

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

    companion object {
        fun from(clickId: String?, referralCode: String?): AttributionEvidence? {
            return when {
                clickId != null -> AttributionEvidence(EvidenceType.CLICK, clickId)
                referralCode != null -> AttributionEvidence(EvidenceType.REFERRAL_CODE, referralCode)
                else -> null
            }
        }
    }
}

data class AttributionConfig(
    val attributionWindow: Duration,
    val strategy: AttributionStrategy,
    val revocationWindowDays: Int = 30,
)

sealed class AttributionResult {
    data class Attributed(val partnerId: PartnerId) : AttributionResult()
    data object Unattributed : AttributionResult()
}
