package com.partnercommission.commission.application.listener

import com.partnercommission.attribution.domain.event.AttributionDecided
import com.partnercommission.attribution.domain.event.AttributionRevoked
import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.`in`.CalculateCommissionUseCase
import com.partnercommission.commission.application.port.`in`.HandleRevocationUseCase
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AttributionEventListener(
    private val calculateCommissionUseCase: CalculateCommissionUseCase,
    private val handleRevocationUseCase: HandleRevocationUseCase,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun on(event: AttributionRevoked) {
        handleRevocationUseCase.execute(event.attributionDecisionId.value.toString())
    }
}
