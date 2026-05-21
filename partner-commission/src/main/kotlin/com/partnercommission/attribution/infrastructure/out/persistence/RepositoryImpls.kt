package com.partnercommission.attribution.infrastructure.out.persistence

import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Repository

@Repository
class ConversionEventRepositoryImpl(
    private val jpa: ConversionEventJpaRepository,
) : ConversionEventRepository {

    override fun save(event: ConversionEvent): ConversionEvent =
        jpa.save(ConversionEventJpaEntity.from(event)).toDomain()

    override fun findById(id: ConversionEventId): ConversionEvent? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun findByTenantAndExternalId(tenantId: TenantId, externalId: String): ConversionEvent? =
        jpa.findByTenantIdAndExternalId(tenantId.value, externalId)?.toDomain()
}

@Repository
class AttributionDecisionRepositoryImpl(
    private val jpa: AttributionDecisionJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : AttributionDecisionRepository {

    override fun save(decision: AttributionDecision): AttributionDecision {
        val saved = jpa.save(AttributionDecisionJpaEntity.from(decision)).toDomain()
        decision.getAndClearDomainEvents().forEach { eventPublisher.publishEvent(it) }
        return saved
    }

    override fun findById(id: AttributionDecisionId): AttributionDecision? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun findByConversionEventId(conversionEventId: ConversionEventId): AttributionDecision? =
        jpa.findByConversionEventId(conversionEventId.value)?.toDomain()
}
