package com.partnercommission.attribution.domain.event

import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.DomainEvent
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

data class AttributionDecided(
    val attributionDecisionId: AttributionDecisionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: ConversionEventId,
    val amount: Money,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class AttributionFailed(
    val conversionEventId: ConversionEventId,
    val tenantId: TenantId,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class AttributionRevoked(
    val attributionDecisionId: AttributionDecisionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: ConversionEventId,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
