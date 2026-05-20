package com.partnercommission.attribution.domain.event

import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

data class AttributionDecided(
    val attributionDecisionId: AttributionDecisionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: ConversionEventId,
    val amount: Money,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class AttributionFailed(
    val conversionEventId: ConversionEventId,
    val tenantId: TenantId,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)

data class AttributionRevoked(
    val attributionDecisionId: AttributionDecisionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val conversionEventId: ConversionEventId,
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
