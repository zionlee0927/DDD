package com.partnercommission.commission.infrastructure.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommissionJpaRepository : JpaRepository<CommissionJpaEntity, UUID> {
    fun findByAttributionDecisionId(attributionDecisionId: String): CommissionJpaEntity?
}
