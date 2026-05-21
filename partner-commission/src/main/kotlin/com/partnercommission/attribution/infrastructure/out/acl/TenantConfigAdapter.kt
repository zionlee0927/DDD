package com.partnercommission.attribution.infrastructure.out.acl

import com.partnercommission.attribution.application.port.out.LoadTenantConfigPort
import com.partnercommission.attribution.domain.exception.TenantNotFoundException
import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.repository.TenantReadRepository
import org.springframework.stereotype.Component

@Component
class TenantConfigAdapter(
    private val tenantRepository: TenantReadRepository,
) : LoadTenantConfigPort {

    override fun loadConfig(tenantId: TenantId): AttributionConfig {
        val programConfig = tenantRepository.findProgramConfigById(tenantId)
            ?: throw TenantNotFoundException(tenantId)
        return AttributionConfig(
            attributionWindow = programConfig.attributionWindow,
            strategy = AttributionStrategy.valueOf(programConfig.attributionStrategy.name),
        )
    }
}
