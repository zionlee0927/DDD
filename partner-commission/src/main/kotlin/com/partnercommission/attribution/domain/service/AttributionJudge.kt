package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class AttributionConfig(
    val attributionWindow: Duration,
    val strategy: AttributionStrategy,
)

class AttributionJudge {

    fun judge(
        evidence: AttributionEvidence?,
        click: Click?,
        trackingLink: TrackingLink?,
        referralCode: ReferralCode?,
        config: AttributionConfig,
        now: LocalDateTime = LocalDateTime.now(),
    ): AttributionResult {
        if (evidence == null) {
            return AttributionResult.Unattributed
        }

        return when (evidence.type) {
            EvidenceType.CLICK -> judgeByClick(click, trackingLink, config, now)
            EvidenceType.REFERRAL_CODE -> judgeByReferralCode(referralCode, now)
        }
    }

    private fun judgeByClick(
        click: Click?,
        trackingLink: TrackingLink?,
        config: AttributionConfig,
        now: LocalDateTime,
    ): AttributionResult {
        if (click == null || trackingLink == null) {
            return AttributionResult.Unattributed
        }

        // 윈도우 체크
        val windowEnd = click.clickedAt.plus(config.attributionWindow.toMinutes(), ChronoUnit.MINUTES)
        if (now.isAfter(windowEnd)) {
            return AttributionResult.Unattributed
        }

        return AttributionResult.Attributed(trackingLink.partnerId)
    }

    private fun judgeByReferralCode(
        referralCode: ReferralCode?,
        now: LocalDateTime,
    ): AttributionResult {
        if (referralCode == null) {
            return AttributionResult.Unattributed
        }

        // 만료 체크
        if (referralCode.expiresAt != null && now.isAfter(referralCode.expiresAt)) {
            return AttributionResult.Unattributed
        }

        return AttributionResult.Attributed(referralCode.partnerId)
    }
}

sealed class AttributionResult {
    data class Attributed(val partnerId: PartnerId) : AttributionResult()
    data object Unattributed : AttributionResult()
}
