package com.partnercommission.attribution.infrastructure.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConversionEventJpaRepository : JpaRepository<ConversionEventJpaEntity, UUID> {
    fun findByTenantIdAndExternalId(tenantId: UUID, externalId: String): ConversionEventJpaEntity?
}

interface AttributionDecisionJpaRepository : JpaRepository<AttributionDecisionJpaEntity, UUID> {
    fun findByConversionEventId(conversionEventId: UUID): AttributionDecisionJpaEntity?
}
