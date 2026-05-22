package com.partnercommission.commission.domain.repository

import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.shared.domain.value.TenantId

interface CommissionRepository {
    fun save(commission: Commission): Commission
    fun findById(id: CommissionId): Commission?
    fun findByAttributionDecisionId(attributionDecisionId: String): Commission?
}
