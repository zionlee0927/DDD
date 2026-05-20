package com.partnercommission.attribution.domain.repository

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.shared.domain.value.TenantId
import java.util.UUID

interface ConversionEventRepository {
    fun save(event: ConversionEvent): ConversionEvent
    fun findById(id: UUID): ConversionEvent?
    fun findByTenantAndExternalId(tenantId: TenantId, externalId: String): ConversionEvent?
}

interface AttributionDecisionRepository {
    fun save(decision: AttributionDecision): AttributionDecision
    fun findById(id: UUID): AttributionDecision?
    fun findByConversionEventId(conversionEventId: UUID): AttributionDecision?
}
