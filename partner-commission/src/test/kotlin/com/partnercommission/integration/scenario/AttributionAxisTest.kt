package com.partnercommission.integration.scenario

import com.partnercommission.attribution.domain.exception.DuplicateConversionException
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.integration.framework.BaseIntegrationTest
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.repository.ReferralCodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("Attribution 축 기반 시나리오")
class AttributionAxisTest : BaseIntegrationTest() {

    @Autowired lateinit var referralCodeRepository: ReferralCodeRepository

    // === 축 1: 증거 타입 ===
    @Nested
    @DisplayName("축 1. 증거 타입")
    inner class EvidenceTypeAxis {

        @Test
        @DisplayName("CLICK 증거 → ATTRIBUTED")
        fun clickEvidence_attributed() {
            val ctx = baseContext()
            val result = flowExecutor.executeFullFlow(ctx)

            assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
            assertThat(result.decision!!.partnerId).isEqualTo(partnerId)

            // DB 검증
            flushAndClear()
            dbVerifier.assertConversionEventExists(tenantId, ctx.externalId)
            dbVerifier.assertDecisionStatus(result.decision!!.id.value, "ATTRIBUTED")
            dbVerifier.assertDecisionPartnerId(result.decision!!.id.value, partnerId.value)
        }

        @Test
        @DisplayName("REFERRAL_CODE 증거 → ATTRIBUTED")
        fun referralCodeEvidence_attributed() {
            referralCodeRepository.save(ReferralCode.create(tenantId, partnerId, "PARTNER10"))

            val result = flowExecutor.executeConversionWithReferralCode(baseContext(), "PARTNER10")

            assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
            assertThat(result.decision!!.partnerId).isEqualTo(partnerId)
        }
    }

    // === 축 2: 클릭 데이터 ===
    @Nested
    @DisplayName("축 2. 클릭 데이터")
    inner class ClickDataAxis {

        @Test
        @DisplayName("클릭 존재 + 링크 존재 → ATTRIBUTED")
        fun clickAndLink_attributed() {
            val result = flowExecutor.executeFullFlow(baseContext())

            assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        }

        @Test
        @DisplayName("클릭 존재하지 않음 → UNATTRIBUTED")
        fun clickNotFound_unattributed() {
            val result = flowExecutor.executeConversionWithClick(
                baseContext(), clickId = "00000000-0000-0000-0000-000000000000"
            )

            assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
        }
    }

    // === 축 3: 중복 ===
    @Nested
    @DisplayName("축 3. 중복 전환")
    inner class DuplicateAxis {

        @Test
        @DisplayName("동일 externalId 재전송 → DuplicateConversionException")
        fun duplicateConversion_rejected() {
            val ctx = baseContext(externalId = "order-dup")
            flowExecutor.executeFullFlow(ctx)

            assertThatThrownBy {
                flowExecutor.executeFullFlow(ctx.copy(externalId = "order-dup"))
            }.isInstanceOf(DuplicateConversionException::class.java)
        }
    }

    // === 축 4: 추천코드 상태 ===
    @Nested
    @DisplayName("축 4. 추천코드 상태")
    inner class ReferralCodeAxis {

        @Test
        @DisplayName("존재하지 않는 코드 → UNATTRIBUTED")
        fun invalidCode_unattributed() {
            val result = flowExecutor.executeConversionWithReferralCode(baseContext(), "INVALID")

            assertThat(result.decision!!.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
        }
    }
}
