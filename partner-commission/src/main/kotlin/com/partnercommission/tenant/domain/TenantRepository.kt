package com.partnercommission.tenant.domain

import com.partnercommission.shared.domain.TenantId

interface TenantRepository {
    fun save(tenant: Tenant): Tenant
    fun findById(id: TenantId): Tenant?
}
