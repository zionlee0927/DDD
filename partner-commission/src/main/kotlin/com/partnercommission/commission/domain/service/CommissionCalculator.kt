package com.partnercommission.commission.domain.service

import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.RuleType
import com.partnercommission.shared.domain.value.Money
import java.math.RoundingMode

class CommissionCalculator {
    fun calculate(conversionAmount: Money, rule: CommissionRule): Money {
        val calculated = when (rule.type) {
            RuleType.PERCENTAGE -> conversionAmount.amount.multiply(rule.value).divide(
                java.math.BigDecimal("100"), 0, RoundingMode.HALF_UP
            )
            RuleType.FIXED -> rule.value
        }
        return Money(calculated, conversionAmount.currency)
    }
}
