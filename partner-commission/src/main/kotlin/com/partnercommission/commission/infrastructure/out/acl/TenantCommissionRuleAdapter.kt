package com.partnercommission.commission.infrastructure.out.acl

import com.partnercommission.commission.application.port.out.LoadCommissionRulePort
import com.partnercommission.commission.domain.value.CommissionRule
import com.partnercommission.commission.domain.value.RuleType
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.repository.TenantReadRepository
import com.partnercommission.tenant.domain.value.CommissionRuleType
import org.springframework.stereotype.Component

@Component
class TenantCommissionRuleAdapter(
    private val tenantRepository: TenantReadRepository,
) : LoadCommissionRulePort {

    override fun loadRule(tenantId: TenantId): CommissionRule {
        val config = tenantRepository.findProgramConfigById(tenantId)
            ?: throw IllegalArgumentException("Tenant not found: ${tenantId.value}")
        return CommissionRule(
            type = RuleType.valueOf(config.commissionRuleType.name),
            value = config.commissionRuleValue,
        )
    }
}
