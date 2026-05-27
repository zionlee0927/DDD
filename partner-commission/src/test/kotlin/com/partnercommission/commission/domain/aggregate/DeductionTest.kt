package com.partnercommission.commission.domain.aggregate

import com.partnercommission.commission.domain.event.DeductionCreated
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.DeductionId
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class DeductionTest {

    private val tenantId = TenantId(UUID.randomUUID())
    private val partnerId = PartnerId(UUID.randomUUID())
    private val commissionId = CommissionId.generate()

    @Test
    fun `생성 시 DeductionCreated 이벤트 발행`() {
        val deduction = Deduction.create(tenantId, partnerId, commissionId, Money(BigDecimal("5000")))

        assertThat(deduction.tenantId).isEqualTo(tenantId)
        assertThat(deduction.originalCommissionId).isEqualTo(commissionId)
        assertThat(deduction.amount.amount).isEqualByComparingTo(BigDecimal("5000"))
        assertThat(deduction.getAndClearDomainEvents()).hasSize(1)
            .first().isInstanceOf(DeductionCreated::class.java)
    }

    @Test
    fun `reconstitute로 복원`() {
        val id = DeductionId.generate()
        val now = LocalDateTime.now()
        val deduction = Deduction.reconstitute(id, tenantId, partnerId, commissionId, Money(BigDecimal("3000")), now)

        assertThat(deduction.id).isEqualTo(id)
        assertThat(deduction.createdAt).isEqualTo(now)
    }
}
