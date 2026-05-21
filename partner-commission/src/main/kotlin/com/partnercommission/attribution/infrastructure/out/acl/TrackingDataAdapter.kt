package com.partnercommission.attribution.infrastructure.out.acl

import com.partnercommission.attribution.application.port.out.LoadClickPort
import com.partnercommission.attribution.application.port.out.LoadReferralCodePort
import com.partnercommission.attribution.domain.value.ClickData
import com.partnercommission.attribution.domain.value.ReferralCodeData
import com.partnercommission.attribution.domain.value.TrackingLinkData
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.repository.ClickReadRepository
import com.partnercommission.tracking.domain.repository.ReferralCodeReadRepository
import com.partnercommission.tracking.domain.repository.TrackingLinkReadRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TrackingDataAdapter(
    private val clickRepository: ClickReadRepository,
    private val trackingLinkRepository: TrackingLinkReadRepository,
    private val referralCodeRepository: ReferralCodeReadRepository,
) : LoadClickPort, LoadReferralCodePort {

    override fun loadClick(clickId: String): ClickData? {
        val view = clickRepository.findViewById(UUID.fromString(clickId)) ?: return null
        return ClickData(
            clickedAt = view.clickedAt,
            trackingCode = view.trackingCode,
        )
    }

    override fun loadTrackingLink(trackingCode: String): TrackingLinkData? {
        val view = trackingLinkRepository.findViewByTrackingCode(trackingCode) ?: return null
        return TrackingLinkData(partnerId = view.partnerId)
    }

    override fun loadReferralCode(tenantId: TenantId, code: String): ReferralCodeData? {
        val view = referralCodeRepository.findViewByTenantAndCode(tenantId, code) ?: return null
        return ReferralCodeData(
            partnerId = view.partnerId,
            expiresAt = view.expiresAt,
        )
    }
}
