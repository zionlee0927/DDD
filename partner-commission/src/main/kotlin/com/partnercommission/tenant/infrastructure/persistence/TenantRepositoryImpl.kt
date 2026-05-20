package com.partnercommission.tenant.infrastructure.persistence

import com.partnercommission.shared.domain.TenantId
import com.partnercommission.tenant.domain.Tenant
import com.partnercommission.tenant.domain.TenantRepository
import org.springframework.stereotype.Repository

@Repository
class TenantRepositoryImpl(private val jpa: TenantJpaRepository) : TenantRepository {
    override fun save(tenant: Tenant): Tenant = jpa.save(TenantJpaEntity.from(tenant)).toDomain()
    override fun findById(id: TenantId): Tenant? = jpa.findById(id.value).orElse(null)?.toDomain()
}
