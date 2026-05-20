package com.partnercommission.attribution.application.port.`in`

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.application.command.ReceiveConversionCommand

interface ReceiveConversionUseCase {
    fun execute(command: ReceiveConversionCommand): AttributionDecision
}
