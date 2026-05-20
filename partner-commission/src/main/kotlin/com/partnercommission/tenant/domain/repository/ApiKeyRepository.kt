package com.partnercommission.tenant.domain.repository

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.ApiKey

interface ApiKeyRepository {
    fun save(apiKey: ApiKey): ApiKey
    fun findByTenantId(tenantId: TenantId): List<ApiKey>
}
