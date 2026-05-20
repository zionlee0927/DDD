package com.partnercommission.tenant.infrastructure.persistence

import com.partnercommission.shared.domain.TenantId
import com.partnercommission.tenant.domain.ApiKey
import com.partnercommission.tenant.domain.ApiKeyRepository
import org.springframework.stereotype.Repository

@Repository
class ApiKeyRepositoryImpl(private val jpa: ApiKeyJpaRepository) : ApiKeyRepository {
    override fun save(apiKey: ApiKey): ApiKey = jpa.save(ApiKeyJpaEntity.from(apiKey)).toDomain()
    override fun findByTenantId(tenantId: TenantId): List<ApiKey> = jpa.findByTenantId(tenantId.value).map { it.toDomain() }
}
