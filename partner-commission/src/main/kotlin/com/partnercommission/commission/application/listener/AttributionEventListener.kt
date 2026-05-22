package com.partnercommission.commission.application.listener

import com.partnercommission.attribution.domain.event.AttributionDecided
import com.partnercommission.attribution.domain.event.AttributionRevoked
import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.`in`.CalculateCommissionUseCase
import com.partnercommission.commission.application.port.`in`.CancelCommissionUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AttributionEventListener(
    private val calculateCommissionUseCase: CalculateCommissionUseCase,
    private val cancelCommissionUseCase: CancelCommissionUseCase,
) {
    @TransactionalEventListener
    fun on(event: AttributionDecided) {
        calculateCommissionUseCase.execute(
            CalculateCommissionCommand(
                tenantId = event.tenantId,
                partnerId = event.partnerId,
                attributionDecisionId = event.attributionDecisionId.value.toString(),
                conversionAmount = event.amount,
            )
        )
    }

    @TransactionalEventListener
    fun on(event: AttributionRevoked) {
        cancelCommissionUseCase.execute(event.attributionDecisionId.value.toString())
    }
}
