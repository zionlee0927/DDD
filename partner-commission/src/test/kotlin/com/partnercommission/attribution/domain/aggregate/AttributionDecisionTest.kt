package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionDecisionId
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.ConversionEventId
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class AttributionDecisionTest {

    private val tenantId = TenantId()
    private val partnerId = PartnerId()
    private val conversionEventId = ConversionEventId.generate()
    private val evidence = AttributionEvidence(EvidenceType.CLICK, "click-1")

    @Test
    fun `귀속 성공 판정을 생성한다`() {
        val decision = AttributionDecision.attributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
            amount = Money(BigDecimal("50000")),
        )

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(decision.partnerId).isEqualTo(partnerId)
        assertThat(decision.isAttributed()).isTrue()
        assertThat(decision.getAndClearDomainEvents()).hasSize(1)
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
        assertThat(decision.getAndClearDomainEvents()).hasSize(1)
    }

    @Test
    fun `ATTRIBUTED 상태에서 철회할 수 있다`() {
        val decision = AttributionDecision.attributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
            amount = Money(BigDecimal("50000")),
        )
        decision.getAndClearDomainEvents()

        decision.revoke(LocalDateTime.now(), 30)

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.REVOKED)
        assertThat(decision.getAndClearDomainEvents()).hasSize(1)
    }

    @Test
    fun `UNATTRIBUTED 상태에서 철회하면 실패한다`() {
        val decision = AttributionDecision.unattributed(
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            strategy = AttributionStrategy.LAST_CLICK,
        )

        assertThatThrownBy { decision.revoke(LocalDateTime.now(), 30) }
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
            amount = Money(BigDecimal("50000")),
        )
        decision.revoke(LocalDateTime.now(), 30)

        assertThatThrownBy { decision.revoke(LocalDateTime.now(), 30) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `철회 기한 초과 시 실패한다`() {
        val decision = AttributionDecision.reconstitute(
            id = AttributionDecisionId.generate(),
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
            status = AttributionStatus.ATTRIBUTED,
            decidedAt = LocalDateTime.now().minusDays(31),
            amount = Money(BigDecimal("50000")),
        )

        assertThatThrownBy { decision.revoke(LocalDateTime.now(), 30) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("기한")
    }

    @Test
    fun `reconstitute로 복원할 수 있다`() {
        val id = AttributionDecisionId.generate()
        val decision = AttributionDecision.reconstitute(
            id = id,
            tenantId = tenantId,
            conversionEventId = conversionEventId,
            partnerId = partnerId,
            evidence = evidence,
            strategy = AttributionStrategy.LAST_CLICK,
            status = AttributionStatus.ATTRIBUTED,
            decidedAt = LocalDateTime.now(),
            amount = Money(BigDecimal("50000")),
        )

        assertThat(decision.id).isEqualTo(id)
        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
    }
}
