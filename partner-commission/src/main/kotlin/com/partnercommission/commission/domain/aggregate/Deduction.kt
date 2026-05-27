package com.partnercommission.commission.domain.aggregate

import com.partnercommission.commission.domain.event.DeductionCreated
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.DeductionId
import com.partnercommission.shared.domain.AggregateRoot
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

class Deduction private constructor(
    id: DeductionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val originalCommissionId: CommissionId,
    val amount: Money,
    val createdAt: LocalDateTime,
) : AggregateRoot<DeductionId>(id) {

    companion object {
        fun create(
            tenantId: TenantId,
            partnerId: PartnerId,
            originalCommissionId: CommissionId,
            amount: Money,
        ): Deduction {
            val deduction = Deduction(
                id = DeductionId.generate(),
                tenantId = tenantId,
                partnerId = partnerId,
                originalCommissionId = originalCommissionId,
                amount = amount,
                createdAt = LocalDateTime.now(),
            )
            deduction.registerEvent(
                DeductionCreated(
                    deductionId = deduction.id,
                    tenantId = tenantId,
                    partnerId = partnerId,
                    originalCommissionId = originalCommissionId,
                    amount = amount,
                )
            )
            return deduction
        }

        fun reconstitute(
            id: DeductionId,
            tenantId: TenantId,
            partnerId: PartnerId,
            originalCommissionId: CommissionId,
            amount: Money,
            createdAt: LocalDateTime,
        ): Deduction = Deduction(id, tenantId, partnerId, originalCommissionId, amount, createdAt)
    }
}
