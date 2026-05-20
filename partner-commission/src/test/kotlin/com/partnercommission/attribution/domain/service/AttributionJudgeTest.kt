package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class AttributionJudgeTest {

    private val judge = AttributionJudge()
    private val tenantId = TenantId()
    private val partnerId = PartnerId()
    private val config = AttributionConfig(
        attributionWindow = Duration.ofDays(30),
        strategy = AttributionStrategy.LAST_CLICK,
    )

    @Test
    fun `증거가 없으면 UNATTRIBUTED`() {
        val result = judge.judge(
            evidence = null,
            click = null,
            trackingLink = null,
            referralCode = null,
            config = config,
        )

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `클릭 기반 - 윈도우 내 클릭이면 ATTRIBUTED`() {
        val click = Click.create(tenantId, "code-1", "127.0.0.1", "Chrome")
        val trackingLink = TrackingLink.create(tenantId, partnerId, "https://example.com")
        val evidence = AttributionEvidence(EvidenceType.CLICK, click.id.toString())

        val result = judge.judge(
            evidence = evidence,
            click = click,
            trackingLink = trackingLink,
            referralCode = null,
            config = config,
        )

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `클릭 기반 - 윈도우 만료되면 UNATTRIBUTED`() {
        val click = Click.create(tenantId, "code-1", "127.0.0.1", "Chrome")
        val trackingLink = TrackingLink.create(tenantId, partnerId, "https://example.com")
        val evidence = AttributionEvidence(EvidenceType.CLICK, click.id.toString())
        val expiredTime = LocalDateTime.now().plusDays(31)

        val result = judge.judge(
            evidence = evidence,
            click = click,
            trackingLink = trackingLink,
            referralCode = null,
            config = config,
            now = expiredTime,
        )

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `클릭 기반 - 클릭이 없으면 UNATTRIBUTED`() {
        val evidence = AttributionEvidence(EvidenceType.CLICK, "nonexistent")

        val result = judge.judge(
            evidence = evidence,
            click = null,
            trackingLink = null,
            referralCode = null,
            config = config,
        )

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `추천코드 기반 - 유효한 코드면 ATTRIBUTED`() {
        val referralCode = ReferralCode.create(tenantId, partnerId, "JISOO10")
        val evidence = AttributionEvidence(EvidenceType.REFERRAL_CODE, "JISOO10")

        val result = judge.judge(
            evidence = evidence,
            click = null,
            trackingLink = null,
            referralCode = referralCode,
            config = config,
        )

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `추천코드 기반 - 코드가 없으면 UNATTRIBUTED`() {
        val evidence = AttributionEvidence(EvidenceType.REFERRAL_CODE, "INVALID")

        val result = judge.judge(
            evidence = evidence,
            click = null,
            trackingLink = null,
            referralCode = null,
            config = config,
        )

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }
}
