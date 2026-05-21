package com.partnercommission.attribution.application.service.processor

import com.partnercommission.attribution.application.port.out.LoadClickPort
import com.partnercommission.attribution.application.port.out.LoadReferralCodePort
import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.ClickData
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.attribution.domain.value.ReferralCodeData
import com.partnercommission.attribution.domain.value.TrackingLinkData
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ClickAttributionProcessorTest {

    private val loadClickPort: LoadClickPort = mockk()
    private val processor = ClickAttributionProcessor(loadClickPort)
    private val tenantId = TenantId()
    private val config = AttributionConfig(Duration.ofDays(30), AttributionStrategy.LAST_CLICK)

    @Test
    fun `클릭과 트래킹링크가 있으면 ATTRIBUTED`() {
        val partnerId = PartnerId()
        every { loadClickPort.loadClick("click-1") } returns ClickData(LocalDateTime.now(), "code-1")
        every { loadClickPort.loadTrackingLink("code-1") } returns TrackingLinkData(partnerId)

        val evidence = AttributionEvidence(EvidenceType.CLICK, "click-1")
        val result = processor.attribute(evidence, tenantId, config)

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `클릭이 없으면 UNATTRIBUTED`() {
        every { loadClickPort.loadClick("click-x") } returns null

        val evidence = AttributionEvidence(EvidenceType.CLICK, "click-x")
        val result = processor.attribute(evidence, tenantId, config)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `supports는 CLICK만 true`() {
        assertThat(processor.supports(EvidenceType.CLICK)).isTrue()
        assertThat(processor.supports(EvidenceType.REFERRAL_CODE)).isFalse()
    }
}

class ReferralCodeAttributionProcessorTest {

    private val loadReferralCodePort: LoadReferralCodePort = mockk()
    private val processor = ReferralCodeAttributionProcessor(loadReferralCodePort)
    private val tenantId = TenantId()
    private val config = AttributionConfig(Duration.ofDays(30), AttributionStrategy.LAST_CLICK)

    @Test
    fun `유효한 추천코드면 ATTRIBUTED`() {
        val partnerId = PartnerId()
        every { loadReferralCodePort.loadReferralCode(tenantId, "CODE1") } returns ReferralCodeData(partnerId, null)

        val evidence = AttributionEvidence(EvidenceType.REFERRAL_CODE, "CODE1")
        val result = processor.attribute(evidence, tenantId, config)

        assertThat(result).isInstanceOf(AttributionResult.Attributed::class.java)
        assertThat((result as AttributionResult.Attributed).partnerId).isEqualTo(partnerId)
    }

    @Test
    fun `추천코드가 없으면 UNATTRIBUTED`() {
        every { loadReferralCodePort.loadReferralCode(tenantId, "INVALID") } returns null

        val evidence = AttributionEvidence(EvidenceType.REFERRAL_CODE, "INVALID")
        val result = processor.attribute(evidence, tenantId, config)

        assertThat(result).isInstanceOf(AttributionResult.Unattributed::class.java)
    }

    @Test
    fun `supports는 REFERRAL_CODE만 true`() {
        assertThat(processor.supports(EvidenceType.REFERRAL_CODE)).isTrue()
        assertThat(processor.supports(EvidenceType.CLICK)).isFalse()
    }
}
