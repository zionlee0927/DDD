package com.partnercommission.commission.infrastructure.out.persistence

import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.value.CommissionId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Repository

@Repository
class CommissionRepositoryImpl(
    private val jpa: CommissionJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : CommissionRepository {

    override fun save(commission: Commission): Commission {
        val saved = jpa.save(CommissionJpaEntity.from(commission)).toDomain()
        commission.getAndClearDomainEvents().forEach { eventPublisher.publishEvent(it) }
        return saved
    }

    override fun findById(id: CommissionId): Commission? =
        jpa.findById(id.value).orElse(null)?.toDomain()

    override fun findByAttributionDecisionId(attributionDecisionId: String): Commission? =
        jpa.findByAttributionDecisionId(attributionDecisionId)?.toDomain()
}
