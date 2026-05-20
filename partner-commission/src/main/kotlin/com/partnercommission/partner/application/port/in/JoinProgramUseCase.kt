package com.partnercommission.partner.application.port.`in`

import com.partnercommission.partner.domain.aggregate.Membership
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId

interface JoinProgramUseCase {
    fun execute(partnerId: PartnerId, tenantId: TenantId): Membership
}
