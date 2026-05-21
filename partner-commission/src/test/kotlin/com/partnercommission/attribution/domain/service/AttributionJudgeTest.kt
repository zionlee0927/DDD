package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.ClickData
import com.partnercommission.attribution.domain.value.ReferralCodeData
import com.partnercommission.attribution.domain.value.TrackingLinkData
import com.partnercommission.shared.domain.value.PartnerId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ClickAttributionJudgeTest {

    private val judge = ClickAttributionJudge()
    private val partnerId = PartnerId()
    private val config = AttributionConfig(
        attributionWindow = Duration.ofDays(30),
        strategy = AttributionStrategy.LAST_CLICK,
    )

    @Test
    fun `윈도우 내 클릭이면 ATTRIBUTED`() {
        val click = ClickData(clickedAt = LocalDateTime.now(), trackingCode = "code-1")
        val trackingLink = TrackingLinkData(partnerId = partnerId)

        val result = judge.judge(click, trackingLink, config)

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `윈도우 만료되면 UNATTRIBUTED`() {
        val click = ClickData(clickedAt = LocalDateTime.now().minusDays(31), trackingCode = "code-1")
        val trackingLink = TrackingLinkData(partnerId = partnerId)

        val result = judge.judge(click, trackingLink, config)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `트래킹링크가 없으면 UNATTRIBUTED`() {
        val click = ClickData(clickedAt = LocalDateTime.now(), trackingCode = "code-1")

        val result = judge.judge(click, null, config)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }
}

class ReferralCodeAttributionJudgeTest {

    private val judge = ReferralCodeAttributionJudge()
    private val partnerId = PartnerId()

    @Test
    fun `유효한 코드면 ATTRIBUTED`() {
        val referralCode = ReferralCodeData(partnerId = partnerId, expiresAt = null)

        val result = judge.judge(referralCode)

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `코드가 없으면 UNATTRIBUTED`() {
        val result = judge.judge(null)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `만료된 코드면 UNATTRIBUTED`() {
        val referralCode = ReferralCodeData(partnerId = partnerId, expiresAt = LocalDateTime.now().minusDays(1))

        val result = judge.judge(referralCode)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }
}
