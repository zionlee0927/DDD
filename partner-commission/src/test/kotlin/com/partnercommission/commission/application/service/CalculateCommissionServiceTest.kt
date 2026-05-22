package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.out.LoadCommissionRulePort
import com.partnercommission.commission.domain.aggregate.Commission
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
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class CalculateCommissionServiceTest {

    private val commissionRepository: CommissionRepository = mockk()
    private val loadCommissionRulePort: LoadCommissionRulePort = mockk()
    private val service = CalculateCommissionService(commissionRepository, loadCommissionRulePort)

    private val tenantId = TenantId(UUID.randomUUID())
    private val partnerId = PartnerId(UUID.randomUUID())
    private val rule = CommissionRule(RuleType.PERCENTAGE, BigDecimal("10"))

    @Test
    fun `정상 산정`() {
        every { loadCommissionRulePort.loadRule(tenantId) } returns rule
        val saved = slot<Commission>()
        every { commissionRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.execute(
            CalculateCommissionCommand(tenantId, partnerId, "attr-1", Money(BigDecimal("50000")))
        )

        assertThat(result.currentStatus()).isEqualTo(CommissionStatus.PENDING)
        assertThat(result.amount.amount).isEqualByComparingTo(BigDecimal("5000"))
    }
}
