package com.partnercommission.tenant.application.port.`in`

import com.partnercommission.tenant.domain.aggregate.Tenant

interface RegisterTenantUseCase {
    fun execute(name: String): Tenant
}
