package com.partnercommission.tracking.application.port.`in`

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.TrackingLink

interface CreateTrackingLinkUseCase {
    fun execute(tenantId: TenantId, partnerId: PartnerId, targetUrl: String): TrackingLink
}
