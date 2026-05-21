package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.ClickData
import com.partnercommission.attribution.domain.value.ReferralCodeData
import com.partnercommission.attribution.domain.value.TrackingLinkData
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ClickAttributionJudge {
    fun judge(click: ClickData, trackingLink: TrackingLinkData?, config: AttributionConfig, now: LocalDateTime = LocalDateTime.now()): AttributionResult {
        trackingLink ?: return AttributionResult.Unattributed

        val windowEnd = click.clickedAt.plus(config.attributionWindow.toMinutes(), ChronoUnit.MINUTES)
        if (now.isAfter(windowEnd)) {
            return AttributionResult.Unattributed
        }

        return AttributionResult.Attributed(trackingLink.partnerId)
    }
}

class ReferralCodeAttributionJudge {
    fun judge(referralCode: ReferralCodeData?, now: LocalDateTime = LocalDateTime.now()): AttributionResult {
        referralCode ?: return AttributionResult.Unattributed

        if (referralCode.expiresAt != null && now.isAfter(referralCode.expiresAt)) {
            return AttributionResult.Unattributed
        }

        return AttributionResult.Attributed(referralCode.partnerId)
    }
}
