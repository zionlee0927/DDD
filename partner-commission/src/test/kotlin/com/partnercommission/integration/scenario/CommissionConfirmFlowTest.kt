package com.partnercommission.integration.scenario

import com.partnercommission.commission.application.port.`in`.ConfirmCommissionCommand
import com.partnercommission.commission.application.port.`in`.ConfirmCommissionUseCase
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.value.CommissionStatus
import com.partnercommission.integration.framework.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("커미션 확정 흐름")
class CommissionConfirmFlowTest : BaseIntegrationTest() {

    @Autowired lateinit var confirmCommissionUseCase: ConfirmCommissionUseCase
    @Autowired lateinit var commissionRepository: CommissionRepository

    @Test
    @DisplayName("귀속 → 커미션 산정 → 확정 → CONFIRMED")
    fun confirmAfterAttribution() {
        val ctx = baseContext()
        val result = flowExecutor.executeFullFlow(ctx)

        // 커미션이 산정되었는지 확인 (동기 이벤트 처리)
        flushAndClear()
        val commission = commissionRepository.findByAttributionDecisionId(result.decision!!.id.value.toString())
        assertThat(commission).isNotNull
        assertThat(commission!!.currentStatus()).isEqualTo(CommissionStatus.PENDING)

        // 확정
        val confirmed = confirmCommissionUseCase.execute(
            ConfirmCommissionCommand(tenantId, result.decision!!.id.value.toString())
        )

        assertThat(confirmed.currentStatus()).isEqualTo(CommissionStatus.CONFIRMED)
    }
}
