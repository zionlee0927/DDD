package com.partnercommission.commission.infrastructure.out.persistence

import com.partnercommission.commission.domain.aggregate.Deduction
import com.partnercommission.commission.domain.repository.DeductionRepository
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.DeductionId
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Repository

@Repository
class DeductionRepositoryImpl(
    private val jpaRepository: DeductionJpaRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : DeductionRepository {

    override fun save(deduction: Deduction): Deduction {
        jpaRepository.save(toEntity(deduction))
        deduction.getAndClearDomainEvents().forEach { eventPublisher.publishEvent(it) }
        return deduction
    }

    override fun findById(id: DeductionId): Deduction? =
        jpaRepository.findById(id.value).orElse(null)?.let { toDomain(it) }

    override fun findByOriginalCommissionId(commissionId: CommissionId): Deduction? =
        jpaRepository.findByOriginalCommissionId(commissionId.value)?.let { toDomain(it) }

    private fun toEntity(d: Deduction) = DeductionJpaEntity(
        id = d.id.value,
        tenantId = d.tenantId.value,
        partnerId = d.partnerId.value,
        originalCommissionId = d.originalCommissionId.value,
        amount = d.amount.amount,
        currency = d.amount.currency,
        createdAt = d.createdAt,
    )

    private fun toDomain(e: DeductionJpaEntity) = Deduction.reconstitute(
        id = DeductionId(e.id),
        tenantId = TenantId(e.tenantId),
        partnerId = PartnerId(e.partnerId),
        originalCommissionId = CommissionId(e.originalCommissionId),
        amount = Money(e.amount, e.currency),
        createdAt = e.createdAt,
    )
}
