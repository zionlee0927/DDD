package com.partnercommission.tenant.domain

import com.partnercommission.shared.domain.TenantId

interface ApiKeyRepository {
    fun save(apiKey: ApiKey): ApiKey
    fun findByTenantId(tenantId: TenantId): List<ApiKey>
}
