package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AttributionDecisionTest {

    private val tenantId = TenantId()
    private val partnerId = PartnerId()
    private val conversionEventId = UUID.randomUUID()
    private val evidence = AttributionEvidence(EvidenceType.CLICK, "click-1")

    @Test
    fun `귀속 성공 판정을 생성한다`() {
        val decision = AttributionDecision.attributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
        )

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(decision.partnerId).isEqualTo(partnerId)
        assertThat(decision.isAttributed()).isTrue()
    }

    @Test
    fun `귀속 실패 판정을 생성한다`() {
        val decision = AttributionDecision.unattributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            strategy = AttributionStrategy.LAST_CLICK,
        )

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
        assertThat(decision.partnerId).isNull()
        assertThat(decision.isAttributed()).isFalse()
    }

    @Test
    fun `ATTRIBUTED 상태에서 철회할 수 있다`() {
        val decision = AttributionDecision.attributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
        )

        decision.revoke()

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.REVOKED)
    }

    @Test
    fun `UNATTRIBUTED 상태에서 철회하면 실패한다`() {
        val decision = AttributionDecision.unattributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            strategy = AttributionStrategy.LAST_CLICK,
        )

        assertThatThrownBy { decision.revoke() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `이미 REVOKED 상태에서 다시 철회하면 실패한다`() {
        val decision = AttributionDecision.attributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
        )
        decision.revoke()

        assertThatThrownBy { decision.revoke() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `reconstitute로 복원할 수 있다`() {
        val id = UUID.randomUUID()
        val decision = AttributionDecision.reconstitute(
            id = id,
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
            status = AttributionStatus.ATTRIBUTED,
            decidedAt = LocalDateTime.now(),
        )

        assertThat(decision.id).isEqualTo(id)
        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
    }
}
