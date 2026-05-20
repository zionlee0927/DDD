package com.partnercommission.tenant.application

import com.partnercommission.tenant.domain.Tenant
import com.partnercommission.tenant.domain.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterTenantUseCase(private val tenantRepository: TenantRepository) {
    fun execute(name: String): Tenant {
        val tenant = Tenant.create(name)
        return tenantRepository.save(tenant)
    }
}
