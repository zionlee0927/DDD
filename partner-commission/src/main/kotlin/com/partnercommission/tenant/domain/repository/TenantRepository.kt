package com.partnercommission.tenant.domain.repository

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.Tenant
import com.partnercommission.tenant.domain.value.ProgramConfig

// Read Repository (타 BC 노출용)
interface TenantReadRepository {
    fun findProgramConfigById(id: TenantId): ProgramConfig?
}

// Full Repository (자기 BC 전용)
interface TenantRepository : TenantReadRepository {
    fun save(tenant: Tenant): Tenant
    fun findById(id: TenantId): Tenant?
}
