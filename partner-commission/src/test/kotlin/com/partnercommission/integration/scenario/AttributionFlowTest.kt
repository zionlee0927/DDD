package com.partnercommission.integration.scenario

import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.integration.framework.BaseIntegrationTest
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.repository.ReferralCodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("정상 귀속 흐름")
class AttributionFlowTest : BaseIntegrationTest() {

    @Autowired lateinit var referralCodeRepository: ReferralCodeRepository

    @Test
    @DisplayName("클릭 기반 → ATTRIBUTED + 파트너 매칭")
    fun clickAttribution() {
        val ctx = baseContext()
        val result = flowExecutor.executeFullFlow(ctx)

        assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(result.decision!!.partnerId).isEqualTo(partnerId)

        flushAndClear()
        dbVerifier.assertConversionEventExists(tenantId, ctx.externalId)
        dbVerifier.assertDecisionStatus(result.decision!!.id.value, "ATTRIBUTED")
        dbVerifier.assertDecisionPartnerId(result.decision!!.id.value, partnerId.value)
    }

    @Test
    @DisplayName("추천코드 기반 → ATTRIBUTED")
    fun referralCodeAttribution() {
        referralCodeRepository.save(ReferralCode.create(tenantId, partnerId, "PARTNER10"))

        val result = flowExecutor.executeConversionWithReferralCode(baseContext(), "PARTNER10")

        assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(result.decision!!.partnerId).isEqualTo(partnerId)
    }

    @Test
    @DisplayName("클릭 미존재 → UNATTRIBUTED")
    fun clickNotFound() {
        val result = flowExecutor.executeConversionWithClick(
            baseContext(), clickId = "00000000-0000-0000-0000-000000000000"
        )

        assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
    }

    @Test
    @DisplayName("존재하지 않는 추천코드 → UNATTRIBUTED")
    fun invalidReferralCode() {
        val result = flowExecutor.executeConversionWithReferralCode(baseContext(), "INVALID")

        assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
    }
}
