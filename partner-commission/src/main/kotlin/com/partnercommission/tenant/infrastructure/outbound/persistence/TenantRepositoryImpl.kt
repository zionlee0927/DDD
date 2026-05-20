package com.partnercommission.tenant.infrastructure.outbound.persistence

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.Tenant
import com.partnercommission.tenant.domain.repository.TenantRepository
import com.partnercommission.tenant.domain.value.ProgramConfig
import org.springframework.stereotype.Repository

@Repository
class TenantRepositoryImpl(private val jpa: TenantJpaRepository) : TenantRepository {
    override fun save(tenant: Tenant): Tenant = jpa.save(TenantJpaEntity.from(tenant)).toDomain()
    override fun findById(id: TenantId): Tenant? = jpa.findById(id.value).orElse(null)?.toDomain()
    override fun findProgramConfigById(id: TenantId): ProgramConfig? = findById(id)?.programConfig
}
