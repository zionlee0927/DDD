package com.partnercommission.attribution.application.port.out

import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.shared.domain.value.TenantId

interface LoadTenantConfigPort {
    fun loadConfig(tenantId: TenantId): AttributionConfig
}
