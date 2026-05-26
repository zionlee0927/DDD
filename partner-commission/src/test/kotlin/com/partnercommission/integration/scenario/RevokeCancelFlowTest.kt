package com.partnercommission.integration.scenario

import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.`in`.CalculateCommissionUseCase
import com.partnercommission.commission.application.port.`in`.HandleRevocationUseCase
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.value.CommissionStatus
import com.partnercommission.integration.framework.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("귀속 철회 → 커미션 취소 흐름")
class RevokeCancelFlowTest : BaseIntegrationTest() {

    @Autowired lateinit var calculateCommissionUseCase: CalculateCommissionUseCase
    @Autowired lateinit var handleRevocationUseCase: HandleRevocationUseCase
    @Autowired lateinit var commissionRepository: CommissionRepository

    @Test
    @DisplayName("PENDING 커미션 → 철회 → CANCELLED")
    fun revokePendingCommission() {
        val ctx = baseContext()
        val result = flowExecutor.executeFullFlow(ctx)

        // 직접 커미션 산정
        calculateCommissionUseCase.execute(
            CalculateCommissionCommand(
                tenantId = tenantId,
                partnerId = partnerId,
                attributionDecisionId = result.decision!!.id.value.toString(),
                conversionAmount = result.decision!!.amount,
            )
        )

        // 철회 처리
        handleRevocationUseCase.execute(result.decision!!.id.value.toString())

        // CANCELLED 확인
        flushAndClear()
        val cancelled = commissionRepository.findByAttributionDecisionId(result.decision!!.id.value.toString())
        assertThat(cancelled!!.currentStatus()).isEqualTo(CommissionStatus.CANCELLED)
    }
}
