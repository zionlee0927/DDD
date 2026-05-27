package com.partnercommission.commission.infrastructure.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeductionJpaRepository : JpaRepository<DeductionJpaEntity, UUID> {
    fun findByOriginalCommissionId(originalCommissionId: UUID): DeductionJpaEntity?
}
