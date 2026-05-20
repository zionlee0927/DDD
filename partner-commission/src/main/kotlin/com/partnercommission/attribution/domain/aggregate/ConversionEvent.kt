package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.AggregateRoot
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

class ConversionEvent private constructor(
    id: ConversionEventId,
    val tenantId: TenantId,
    val externalId: String,
    val amount: Money,
    val eventType: String,
    val evidence: AttributionEvidence?,
    val receivedAt: LocalDateTime,
) : AggregateRoot<ConversionEventId>(id) {

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
                id = ConversionEventId.generate(),
                tenantId = tenantId,
                externalId = externalId,
                amount = amount,
                eventType = eventType,
                evidence = evidence,
                receivedAt = LocalDateTime.now(),
            )
        }

        fun reconstitute(
            id: ConversionEventId,
            tenantId: TenantId,
            externalId: String,
            amount: Money,
            eventType: String,
            evidence: AttributionEvidence?,
            receivedAt: LocalDateTime,
        ): ConversionEvent = ConversionEvent(id, tenantId, externalId, amount, eventType, evidence, receivedAt)
    }
}
