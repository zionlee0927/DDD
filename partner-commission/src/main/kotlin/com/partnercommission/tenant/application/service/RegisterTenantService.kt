package com.partnercommission.tenant.application.service

import com.partnercommission.tenant.application.port.`in`.RegisterTenantUseCase
import com.partnercommission.tenant.domain.aggregate.Tenant
import com.partnercommission.tenant.domain.repository.TenantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterTenantService(private val tenantRepository: TenantRepository) : RegisterTenantUseCase {
    override fun execute(name: String): Tenant {
        val tenant = Tenant.create(name)
        return tenantRepository.save(tenant)
    }
}
