package com.partnercommission.commission.application.port.out

import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.shared.domain.value.TenantId

interface LoadCommissionRulePort {
    fun loadRule(tenantId: TenantId): CommissionRule
}
