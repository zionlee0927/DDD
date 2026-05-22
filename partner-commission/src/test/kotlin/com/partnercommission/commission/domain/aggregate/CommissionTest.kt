package com.partnercommission.commission.domain.aggregate

import com.partnercommission.commission.domain.event.CommissionCalculated
import com.partnercommission.commission.domain.event.CommissionCancelled
import com.partnercommission.commission.domain.event.CommissionConfirmed
import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.CommissionStatus
import com.partnercommission.commission.domain.value.RuleType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class CommissionTest {

    private val tenantId = TenantId(UUID.randomUUID())
    private val partnerId = PartnerId(UUID.randomUUID())
    private val rule = CommissionRule(RuleType.PERCENTAGE, BigDecimal("10"))

    private fun createCommission() = Commission.create(
        tenantId = tenantId,
        partnerId = partnerId,
        attributionDecisionId = "attr-1",
        amount = Money(BigDecimal("5000")),
        rule = rule,
    )

    @Test
    fun `생성 시 PENDING + CommissionCalculated 이벤트`() {
        val commission = createCommission()

        assertThat(commission.currentStatus()).isEqualTo(CommissionStatus.PENDING)
        assertThat(commission.getAndClearDomainEvents()).hasSize(1)
            .first().isInstanceOf(CommissionCalculated::class.java)
    }

    @Test
    fun `confirm → CONFIRMED + CommissionConfirmed 이벤트`() {
        val commission = createCommission()
        commission.getAndClearDomainEvents()

        commission.confirm()

        assertThat(commission.currentStatus()).isEqualTo(CommissionStatus.CONFIRMED)
        assertThat(commission.getAndClearDomainEvents()).hasSize(1)
            .first().isInstanceOf(CommissionConfirmed::class.java)
    }

    @Test
    fun `cancel → CANCELLED + CommissionCancelled 이벤트`() {
        val commission = createCommission()
        commission.getAndClearDomainEvents()

        commission.cancel()

        assertThat(commission.currentStatus()).isEqualTo(CommissionStatus.CANCELLED)
        assertThat(commission.getAndClearDomainEvents()).hasSize(1)
            .first().isInstanceOf(CommissionCancelled::class.java)
    }

    @Test
    fun `CONFIRMED 상태에서 confirm 시 실패`() {
        val commission = createCommission()
        commission.confirm()

        assertThatThrownBy { commission.confirm() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CONFIRMED 상태에서 cancel 시 실패`() {
        val commission = createCommission()
        commission.confirm()

        assertThatThrownBy { commission.cancel() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CANCELLED 상태에서 confirm 시 실패`() {
        val commission = createCommission()
        commission.cancel()

        assertThatThrownBy { commission.confirm() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `reconstitute로 복원`() {
        val commission = createCommission()
        val restored = Commission.reconstitute(
            id = commission.id,
            tenantId = tenantId,
            partnerId = partnerId,
            attributionDecisionId = "attr-1",
            amount = Money(BigDecimal("5000")),
            rule = rule,
            status = CommissionStatus.CONFIRMED,
            calculatedAt = commission.calculatedAt,
        )
        assertThat(restored.currentStatus()).isEqualTo(CommissionStatus.CONFIRMED)
    }
}
