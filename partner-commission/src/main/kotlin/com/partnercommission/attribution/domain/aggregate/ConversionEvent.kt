package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

class ConversionEvent private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val externalId: String,
    val amount: Money,
    val eventType: String,
    val evidence: AttributionEvidence?,
    val receivedAt: LocalDateTime,
) {
    companion object {
        fun create(
            tenantId: TenantId,
            externalId: String,
            amount: Money,
            eventType: String,
            evidence: AttributionEvidence?,
        ): ConversionEvent {
            require(externalId.isNotBlank()) { "externalId는 비어있을 수 없다" }
            require(eventType.isNotBlank()) { "eventType은 비어있을 수 없다" }
            return ConversionEvent(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                externalId = externalId,
                amount = amount,
                eventType = eventType,
                evidence = evidence,
                receivedAt = LocalDateTime.now(),
            )
        }

        fun reconstitute(
            id: UUID,
            tenantId: TenantId,
            externalId: String,
            amount: Money,
            eventType: String,
            evidence: AttributionEvidence?,
            receivedAt: LocalDateTime,
        ): ConversionEvent = ConversionEvent(id, tenantId, externalId, amount, eventType, evidence, receivedAt)
    }
}
