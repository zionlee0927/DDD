package com.partnercommission.attribution.application.port.`in`

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.shared.domain.value.TenantId
import java.math.BigDecimal

interface ReceiveConversionUseCase {
    fun execute(command: ReceiveConversionCommand): AttributionDecision
}

data class ReceiveConversionCommand(
    val tenantId: TenantId,
    val externalId: String,
    val amount: BigDecimal,
    val eventType: String,
    val clickId: String?,
    val referralCode: String?,
)
