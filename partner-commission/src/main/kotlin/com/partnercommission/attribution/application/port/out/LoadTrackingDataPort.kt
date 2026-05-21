package com.partnercommission.attribution.application.port.out

import com.partnercommission.attribution.domain.value.ClickData
import com.partnercommission.attribution.domain.value.ReferralCodeData
import com.partnercommission.attribution.domain.value.TrackingLinkData
import com.partnercommission.shared.domain.value.TenantId

interface LoadClickPort {
    fun loadClick(clickId: String): ClickData?
    fun loadTrackingLink(trackingCode: String): TrackingLinkData?
}

interface LoadReferralCodePort {
    fun loadReferralCode(tenantId: TenantId, code: String): ReferralCodeData?
}
