package com.partnercommission.tenant.application.port.`in`

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.ApiKey

interface IssueApiKeyUseCase {
    fun execute(tenantId: TenantId): ApiKey
}
