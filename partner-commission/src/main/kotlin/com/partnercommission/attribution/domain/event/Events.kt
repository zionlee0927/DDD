package com.partnercommission.attribution.domain.event

import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

data class AttributionDecided(
    val attributionDecisionId: UUID,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: UUID,
    val amount: Money,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class AttributionFailed(
    val conversionEventId: UUID,
    val tenantId: TenantId,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class AttributionRevoked(
    val attributionDecisionId: UUID,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: UUID,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
