package com.partnercommission.commission.application.service

import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.CommissionStatus
import com.partnercommission.commission.domain.value.RuleType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class CancelCommissionServiceTest {

    private val commissionRepository: CommissionRepository = mockk()
    private val service = CancelCommissionService(commissionRepository)

    private val attrId = "attr-1"

    private fun pendingCommission() = Commission.create(
        tenantId = TenantId(UUID.randomUUID()),
        partnerId = PartnerId(UUID.randomUUID()),
        attributionDecisionId = attrId,
        amount = Money(BigDecimal("5000")),
        rule = CommissionRule(RuleType.PERCENTAGE, BigDecimal("10")),
    )

    @Test
    fun `정상 취소`() {
        val commission = pendingCommission()
        every { commissionRepository.findByAttributionDecisionId(attrId) } returns commission
        val saved = slot<Commission>()
        every { commissionRepository.save(capture(saved)) } answers { saved.captured }

        service.execute(attrId)

        assertThat(commission.currentStatus()).isEqualTo(CommissionStatus.CANCELLED)
    }

    @Test
    fun `커미션 없으면 예외`() {
        every { commissionRepository.findByAttributionDecisionId(attrId) } returns null

        assertThatThrownBy { service.execute(attrId) }
            .isInstanceOf(CommissionNotFoundByAttributionException::class.java)
    }
}
