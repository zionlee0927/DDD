package com.partnercommission.commission.application.port.`in`

import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId

interface CalculateCommissionUseCase {
    fun execute(command: CalculateCommissionCommand): Commission
}

data class CalculateCommissionCommand(
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val attributionDecisionId: String,
    val conversionAmount: Money,
)

interface CancelCommissionUseCase {
    fun execute(attributionDecisionId: String)
}

interface ConfirmCommissionUseCase {
    fun execute(command: ConfirmCommissionCommand): Commission
}

data class ConfirmCommissionCommand(
    val tenantId: TenantId,
    val attributionDecisionId: String,
)
