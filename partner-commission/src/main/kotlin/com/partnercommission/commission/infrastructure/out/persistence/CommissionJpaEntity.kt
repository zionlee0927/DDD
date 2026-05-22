package com.partnercommission.commission.infrastructure.out.persistence

import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.value.*
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "commissions")
class CommissionJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val partnerId: UUID,
    val attributionDecisionId: String,
    val amount: BigDecimal,
    val currency: String,
    @Enumerated(EnumType.STRING) val ruleType: RuleType,
    val ruleValue: BigDecimal,
    @Enumerated(EnumType.STRING) val status: CommissionStatus,
    val calculatedAt: LocalDateTime,
) {
    fun toDomain(): Commission = Commission.reconstitute(
        id = CommissionId(id),
        tenantId = TenantId(tenantId),
        partnerId = PartnerId(partnerId),
        attributionDecisionId = attributionDecisionId,
        amount = Money(amount, currency),
        rule = CommissionRule(ruleType, ruleValue),
        status = status,
        calculatedAt = calculatedAt,
    )

    companion object {
        fun from(c: Commission) = CommissionJpaEntity(
            id = c.id.value,
            tenantId = c.tenantId.value,
            partnerId = c.partnerId.value,
            attributionDecisionId = c.attributionDecisionId,
            amount = c.amount.amount,
            currency = c.amount.currency,
            ruleType = c.rule.type,
            ruleValue = c.rule.value,
            status = c.currentStatus(),
            calculatedAt = c.calculatedAt,
        )
    }
}
