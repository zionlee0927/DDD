package com.partnercommission.commission.domain.aggregate

import com.partnercommission.commission.domain.event.CommissionCalculated
import com.partnercommission.commission.domain.event.CommissionCancelled
import com.partnercommission.commission.domain.event.CommissionConfirmed
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.CommissionStatus
import com.partnercommission.shared.domain.AggregateRoot
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

class Commission private constructor(
    id: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val attributionDecisionId: String,
    val amount: Money,
    val rule: CommissionRule,
    private var status: CommissionStatus,
    val calculatedAt: LocalDateTime,
) : AggregateRoot<CommissionId>(id) {

    fun currentStatus(): CommissionStatus = status

    fun cancel() {
        check(status == CommissionStatus.PENDING) { "PENDING 상태에서만 취소 가능" }
        status = CommissionStatus.CANCELLED
        registerEvent(CommissionCancelled(commissionId = id, tenantId = tenantId, partnerId = partnerId))
    }

    fun confirm() {
        check(status == CommissionStatus.PENDING) { "PENDING 상태에서만 확정 가능" }
        status = CommissionStatus.CONFIRMED
        registerEvent(CommissionConfirmed(commissionId = id, tenantId = tenantId, partnerId = partnerId, amount = amount))
    }

    companion object {
        fun create(
            tenantId: TenantId,
            partnerId: PartnerId,
            attributionDecisionId: String,
            amount: Money,
            rule: CommissionRule,
        ): Commission {
            val commission = Commission(
                id = CommissionId.generate(),
                tenantId = tenantId,
                partnerId = partnerId,
                attributionDecisionId = attributionDecisionId,
                amount = amount,
                rule = rule,
                status = CommissionStatus.PENDING,
                calculatedAt = LocalDateTime.now(),
            )
            commission.registerEvent(
                CommissionCalculated(
                    commissionId = commission.id,
                    tenantId = tenantId,
                    partnerId = partnerId,
                    amount = amount,
                )
            )
            return commission
        }

        fun reconstitute(
            id: CommissionId,
            tenantId: TenantId,
            partnerId: PartnerId,
            attributionDecisionId: String,
            amount: Money,
            rule: CommissionRule,
            status: CommissionStatus,
            calculatedAt: LocalDateTime,
        ): Commission = Commission(id, tenantId, partnerId, attributionDecisionId, amount, rule, status, calculatedAt)
    }
}
