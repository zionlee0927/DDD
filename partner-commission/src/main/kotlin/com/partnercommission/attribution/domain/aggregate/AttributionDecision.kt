package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.event.AttributionDecided
import com.partnercommission.attribution.domain.event.AttributionFailed
import com.partnercommission.attribution.domain.event.AttributionRevoked
import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.AggregateRoot
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

class AttributionDecision private constructor(
    id: AttributionDecisionId,
    val tenantId: TenantId,
    val conversionEventId: ConversionEventId,
    val partnerId: PartnerId?,
    val evidence: AttributionEvidence?,
    val strategy: AttributionStrategy,
    private var status: AttributionStatus,
    val decidedAt: LocalDateTime,
    val amount: Money,
) : AggregateRoot<AttributionDecisionId>(id) {

    fun currentStatus(): AttributionStatus = status

    fun revoke() {
        check(status == AttributionStatus.ATTRIBUTED) { "ATTRIBUTED 상태에서만 철회 가능" }
        status = AttributionStatus.REVOKED
        registerEvent(
            AttributionRevoked(
                attributionDecisionId = id,
                tenantId = tenantId,
                partnerId = partnerId!!,
                conversionEventId = conversionEventId,
            )
        )
    }

    fun isAttributed(): Boolean = status == AttributionStatus.ATTRIBUTED

    companion object {
        fun attributed(
            tenantId: TenantId,
            conversionEventId: ConversionEventId,
            partnerId: PartnerId,
            evidence: AttributionEvidence,
            strategy: AttributionStrategy,
            amount: Money,
        ): AttributionDecision {
            val decision = AttributionDecision(
                id = AttributionDecisionId.generate(),
                tenantId = tenantId,
                conversionEventId = conversionEventId,
                partnerId = partnerId,
                evidence = evidence,
                strategy = strategy,
                status = AttributionStatus.ATTRIBUTED,
                decidedAt = LocalDateTime.now(),
                amount = amount,
            )
            decision.registerEvent(
                AttributionDecided(
                    attributionDecisionId = decision.id,
                    tenantId = tenantId,
                    partnerId = partnerId,
                    conversionEventId = conversionEventId,
                    amount = amount,
                )
            )
            return decision
        }

        fun unattributed(
            tenantId: TenantId,
            conversionEventId: ConversionEventId,
            strategy: AttributionStrategy,
        ): AttributionDecision {
            val decision = AttributionDecision(
                id = AttributionDecisionId.generate(),
                tenantId = tenantId,
                conversionEventId = conversionEventId,
                partnerId = null,
                evidence = null,
                strategy = strategy,
                status = AttributionStatus.UNATTRIBUTED,
                decidedAt = LocalDateTime.now(),
                amount = Money.ZERO,
            )
            decision.registerEvent(
                AttributionFailed(
                    conversionEventId = conversionEventId,
                    tenantId = tenantId,
                )
            )
            return decision
        }

        fun reconstitute(
            id: AttributionDecisionId,
            tenantId: TenantId,
            conversionEventId: ConversionEventId,
            partnerId: PartnerId?,
            evidence: AttributionEvidence?,
            strategy: AttributionStrategy,
            status: AttributionStatus,
            decidedAt: LocalDateTime,
            amount: Money,
        ): AttributionDecision = AttributionDecision(id, tenantId, conversionEventId, partnerId, evidence, strategy, status, decidedAt, amount)
    }
}
