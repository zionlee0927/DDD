package com.partnercommission.attribution.domain.aggregate

import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ConversionEventTest {

    @Test
    fun `전환 이벤트를 생성한다`() {
        val event = ConversionEvent.create(
            tenantId = TenantId(),
            externalId = "order-123",
            amount = Money(BigDecimal("50000")),
            eventType = "PAYMENT",
            evidence = AttributionEvidence(EvidenceType.CLICK, "click-1"),
        )

        assertThat(event.externalId).isEqualTo("order-123")
        assertThat(event.eventType).isEqualTo("PAYMENT")
        assertThat(event.evidence?.type).isEqualTo(EvidenceType.CLICK)
    }

    @Test
    fun `증거 없이 전환 이벤트를 생성할 수 있다`() {
        val event = ConversionEvent.create(
            tenantId = TenantId(),
            externalId = "order-456",
            amount = Money(BigDecimal("30000")),
            eventType = "VISIT",
            evidence = null,
        )

        assertThat(event.evidence).isNull()
    }

    @Test
    fun `externalId가 비어있으면 생성 실패`() {
        assertThatThrownBy {
            ConversionEvent.create(
                tenantId = TenantId(),
                externalId = "",
                amount = Money(BigDecimal("10000")),
                eventType = "PAYMENT",
                evidence = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `eventType이 비어있으면 생성 실패`() {
        assertThatThrownBy {
            ConversionEvent.create(
                tenantId = TenantId(),
                externalId = "order-789",
                amount = Money(BigDecimal("10000")),
                eventType = "",
                evidence = null,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `reconstitute로 복원할 수 있다`() {
        val id = UUID.randomUUID()
        val event = ConversionEvent.reconstitute(
            id = id,
            tenantId = TenantId(),
            externalId = "order-100",
            amount = Money(BigDecimal("20000")),
            eventType = "PAYMENT",
            evidence = AttributionEvidence(EvidenceType.REFERRAL_CODE, "CODE1"),
            receivedAt = LocalDateTime.now(),
        )

        assertThat(event.id).isEqualTo(id)
        assertThat(event.externalId).isEqualTo("order-100")
    }
}
