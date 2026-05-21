package com.partnercommission.attribution.infrastructure.out.persistence

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.value.*
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "conversion_events")
class ConversionEventJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val externalId: String,
    val amount: BigDecimal,
    val currency: String,
    val eventType: String,
    val evidenceType: String?,
    val evidenceReferenceId: String?,
    val receivedAt: LocalDateTime,
) {
    fun toDomain(): ConversionEvent = ConversionEvent.reconstitute(
        id = ConversionEventId(id),
        tenantId = TenantId(tenantId),
        externalId = externalId,
        amount = Money(amount, currency),
        eventType = eventType,
        evidence = if (evidenceType != null && evidenceReferenceId != null)
            AttributionEvidence(EvidenceType.valueOf(evidenceType), evidenceReferenceId) else null,
        receivedAt = receivedAt,
    )

    companion object {
        fun from(e: ConversionEvent) = ConversionEventJpaEntity(
            id = e.id.value,
            tenantId = e.tenantId.value,
            externalId = e.externalId,
            amount = e.amount.amount,
            currency = e.amount.currency,
            eventType = e.eventType,
            evidenceType = e.evidence?.type?.name,
            evidenceReferenceId = e.evidence?.referenceId,
            receivedAt = e.receivedAt,
        )
    }
}

@Entity
@Table(name = "attribution_decisions")
class AttributionDecisionJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val conversionEventId: UUID,
    val partnerId: UUID?,
    val evidenceType: String?,
    val evidenceReferenceId: String?,
    @Enumerated(EnumType.STRING) val strategy: AttributionStrategy,
    @Enumerated(EnumType.STRING) val status: AttributionStatus,
    val amount: BigDecimal,
    val currency: String,
    val decidedAt: LocalDateTime,
) {
    fun toDomain(): AttributionDecision = AttributionDecision.reconstitute(
        id = AttributionDecisionId(id),
        tenantId = TenantId(tenantId),
        conversionEventId = ConversionEventId(conversionEventId),
        partnerId = partnerId?.let { PartnerId(it) },
        evidence = if (evidenceType != null && evidenceReferenceId != null)
            AttributionEvidence(EvidenceType.valueOf(evidenceType), evidenceReferenceId) else null,
        strategy = strategy,
        status = status,
        decidedAt = decidedAt,
        amount = Money(amount, currency),
    )

    companion object {
        fun from(d: AttributionDecision) = AttributionDecisionJpaEntity(
            id = d.id.value,
            tenantId = d.tenantId.value,
            conversionEventId = d.conversionEventId.value,
            partnerId = d.partnerId?.value,
            evidenceType = d.evidence?.type?.name,
            evidenceReferenceId = d.evidence?.referenceId,
            strategy = d.strategy,
            status = d.currentStatus(),
            amount = d.amount.amount,
            currency = d.amount.currency,
            decidedAt = d.decidedAt,
        )
    }
}
