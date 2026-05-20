package com.partnercommission.tenant.application.service

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.application.port.`in`.IssueApiKeyUseCase
import com.partnercommission.tenant.domain.aggregate.ApiKey
import com.partnercommission.tenant.domain.repository.ApiKeyRepository
import com.partnercommission.tenant.domain.repository.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class IssueApiKeyService(
    private val tenantRepository: TenantRepository,
    private val apiKeyRepository: ApiKeyRepository
) : IssueApiKeyUseCase {
    override fun execute(tenantId: TenantId): ApiKey {
        tenantRepository.findById(tenantId) ?: throw IllegalArgumentException("Tenant not found")
        val apiKey = ApiKey.create(tenantId)
        return apiKeyRepository.save(apiKey)
    }
}
