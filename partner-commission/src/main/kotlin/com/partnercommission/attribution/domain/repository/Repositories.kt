package com.partnercommission.attribution.domain.repository

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.value.TenantId

interface ConversionEventRepository {
    fun save(event: ConversionEvent): ConversionEvent
    fun findById(id: ConversionEventId): ConversionEvent?
    fun findByTenantAndExternalId(tenantId: TenantId, externalId: String): ConversionEvent?
}

interface AttributionDecisionRepository {
    fun save(decision: AttributionDecision): AttributionDecision
    fun findById(id: AttributionDecisionId): AttributionDecision?
    fun findByConversionEventId(conversionEventId: ConversionEventId): AttributionDecision?
}
