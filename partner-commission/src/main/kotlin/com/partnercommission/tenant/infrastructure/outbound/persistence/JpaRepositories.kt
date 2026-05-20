package com.partnercommission.tenant.infrastructure.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantJpaRepository : JpaRepository<TenantJpaEntity, UUID>

interface ApiKeyJpaRepository : JpaRepository<ApiKeyJpaEntity, UUID> {
    fun findByTenantId(tenantId: UUID): List<ApiKeyJpaEntity>
}
