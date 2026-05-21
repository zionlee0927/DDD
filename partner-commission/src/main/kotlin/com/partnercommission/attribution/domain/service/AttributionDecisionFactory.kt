package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionStrategy

class AttributionDecisionFactory {
    fun create(
        result: AttributionResult,
        conversionEvent: ConversionEvent,
        strategy: AttributionStrategy,
    ): AttributionDecision = when (result) {
        is AttributionResult.Attributed -> AttributionDecision.attributed(
            tenantId = conversionEvent.tenantId,
            conversionEventId = conversionEvent.id,
            partnerId = result.partnerId,
            evidence = conversionEvent.evidence!!,
            strategy = strategy,
            amount = conversionEvent.amount,
        )
        is AttributionResult.Unattributed -> AttributionDecision.unattributed(
            tenantId = conversionEvent.tenantId,
            conversionEventId = conversionEvent.id,
            strategy = strategy,
        )
    }
}
