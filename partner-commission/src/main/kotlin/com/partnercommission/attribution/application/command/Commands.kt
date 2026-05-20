package com.partnercommission.attribution.application.command

import com.partnercommission.shared.domain.value.TenantId
import java.math.BigDecimal

data class ReceiveConversionCommand(
    val tenantId: TenantId,
    val externalId: String,
    val amount: BigDecimal,
    val eventType: String,
    val clickId: String?,
    val referralCode: String?,
)
