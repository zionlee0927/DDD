package com.partnercommission.partner.infrastructure.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PartnerJpaRepository : JpaRepository<PartnerJpaEntity, UUID>

interface MembershipJpaRepository : JpaRepository<MembershipJpaEntity, UUID> {
    fun findByPartnerId(partnerId: UUID): List<MembershipJpaEntity>
}
