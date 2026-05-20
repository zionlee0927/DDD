package com.partnercommission.tenant.application

import com.partnercommission.shared.domain.TenantId
import com.partnercommission.tenant.domain.ApiKey
import com.partnercommission.tenant.domain.ApiKeyRepository
import com.partnercommission.tenant.domain.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class IssueApiKeyUseCase(
    private val tenantRepository: TenantRepository,
    private val apiKeyRepository: ApiKeyRepository
) {
    fun execute(tenantId: TenantId): ApiKey {
        tenantRepository.findById(tenantId) ?: throw IllegalArgumentException("Tenant not found")
        val apiKey = ApiKey.create(tenantId)
        return apiKeyRepository.save(apiKey)
    }
}
