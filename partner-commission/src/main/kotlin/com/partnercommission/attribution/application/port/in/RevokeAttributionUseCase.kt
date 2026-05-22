package com.partnercommission.attribution.application.port.`in`

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.shared.domain.value.TenantId

interface RevokeAttributionUseCase {
    fun execute(command: RevokeAttributionCommand): AttributionDecision
}

data class RevokeAttributionCommand(
    val tenantId: TenantId,
    val externalId: String,
)
