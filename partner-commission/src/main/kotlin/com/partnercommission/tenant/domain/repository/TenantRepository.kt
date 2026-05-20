package com.partnercommission.tenant.domain.repository

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.Tenant

interface TenantRepository {
    fun save(tenant: Tenant): Tenant
    fun findById(id: TenantId): Tenant?
}
