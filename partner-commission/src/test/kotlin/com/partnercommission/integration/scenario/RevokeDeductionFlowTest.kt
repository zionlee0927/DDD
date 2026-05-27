package com.partnercommission.integration.scenario

import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.`in`.CalculateCommissionUseCase
import com.partnercommission.commission.application.port.`in`.ConfirmCommissionCommand
import com.partnercommission.commission.application.port.`in`.ConfirmCommissionUseCase
import com.partnercommission.commission.application.port.`in`.HandleRevocationUseCase
import com.partnercommission.commission.domain.repository.DeductionRepository
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.integration.framework.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("확정 후 철회 → 차감 생성 흐름")
class RevokeDeductionFlowTest : BaseIntegrationTest() {

    @Autowired lateinit var calculateCommissionUseCase: CalculateCommissionUseCase
    @Autowired lateinit var confirmCommissionUseCase: ConfirmCommissionUseCase
    @Autowired lateinit var handleRevocationUseCase: HandleRevocationUseCase
    @Autowired lateinit var deductionRepository: DeductionRepository

    @Test
    @DisplayName("CONFIRMED 커미션 → 철회 → Deduction 생성")
    fun revokeConfirmedCommission_createsDeduction() {
        val ctx = baseContext()
        val result = flowExecutor.executeFullFlow(ctx)
        val attrId = result.decision!!.id.value.toString()

        // 커미션 산정 + 확정
        val commission = calculateCommissionUseCase.execute(
            CalculateCommissionCommand(tenantId, partnerId, attrId, result.decision!!.amount)
        )
        confirmCommissionUseCase.execute(ConfirmCommissionCommand(tenantId, attrId))

        // 철회 → 차감 생성
        handleRevocationUseCase.execute(attrId)

        // Deduction 검증
        flushAndClear()
        val deduction = deductionRepository.findByOriginalCommissionId(commission.id)
        assertThat(deduction).isNotNull
        assertThat(deduction!!.amount).isEqualTo(commission.amount)
        assertThat(deduction.partnerId).isEqualTo(partnerId)
    }
}
