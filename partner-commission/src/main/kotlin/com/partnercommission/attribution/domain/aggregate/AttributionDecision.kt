package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

class AttributionDecision private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val conversionEventId: UUID,
    val partnerId: PartnerId?,
    val evidence: AttributionEvidence?,
    val strategy: AttributionStrategy,
    private var status: AttributionStatus,
    val decidedAt: LocalDateTime,
) {
    fun currentStatus(): AttributionStatus = status

    fun revoke() {
        check(status == AttributionStatus.ATTRIBUTED) { "ATTRIBUTED 상태에서만 철회 가능" }
        status = AttributionStatus.REVOKED
    }

    fun isAttributed(): Boolean = status == AttributionStatus.ATTRIBUTED

    companion object {
        fun attributed(
            tenantId: TenantId,
            conversionEventId: UUID,
            partnerId: PartnerId,
            evidence: AttributionEvidence,
            strategy: AttributionStrategy,
        ): AttributionDecision = AttributionDecision(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = strategy,
            status = AttributionStatus.ATTRIBUTED,
            decidedAt = LocalDateTime.now(),
        )

        fun unattributed(
            tenantId: TenantId,
            conversionEventId: UUID,
            strategy: AttributionStrategy,
        ): AttributionDecision = AttributionDecision(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = null,
            evidence = null,
            strategy = strategy,
            status = AttributionStatus.UNATTRIBUTED,
            decidedAt = LocalDateTime.now(),
        )

        fun reconstitute(
            id: UUID,
            tenantId: TenantId,
            conversionEventId: UUID,
            partnerId: PartnerId?,
            evidence: AttributionEvidence?,
            strategy: AttributionStrategy,
            status: AttributionStatus,
            decidedAt: LocalDateTime,
        ): AttributionDecision = AttributionDecision(id, tenantId, conversionEventId, partnerId, evidence, strategy, status, decidedAt)
    }
}
