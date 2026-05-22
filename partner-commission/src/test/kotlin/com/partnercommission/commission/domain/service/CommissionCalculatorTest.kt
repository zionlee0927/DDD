package com.partnercommission.commission.domain.service

import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.RuleType
import com.partnercommission.shared.domain.value.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class CommissionCalculatorTest {

    private val calculator = CommissionCalculator()

    @Test
    fun `PERCENTAGE 규칙`() {
        val result = calculator.calculate(Money(BigDecimal("50000")), CommissionRule(RuleType.PERCENTAGE, BigDecimal("10")))
        assertThat(result.amount).isEqualByComparingTo(BigDecimal("5000"))
    }

    @Test
    fun `FIXED 규칙`() {
        val result = calculator.calculate(Money(BigDecimal("50000")), CommissionRule(RuleType.FIXED, BigDecimal("3000")))
        assertThat(result.amount).isEqualByComparingTo(BigDecimal("3000"))
    }
}
