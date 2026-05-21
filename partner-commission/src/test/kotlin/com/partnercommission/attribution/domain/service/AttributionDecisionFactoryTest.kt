package com.partnercommission.attribution.domain.service

import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AttributionDecisionFactoryTest {

    private val factory = AttributionDecisionFactory()
    private val tenantId = TenantId()
    private val evidence = AttributionEvidence(EvidenceType.CLICK, "click-1")

    private val conversionEvent = ConversionEvent.create(
        tenantId = tenantId,
        externalId = "order-1",
        amount = Money(BigDecimal("50000")),
        eventType = "PAYMENT",
        evidence = evidence,
    )

    @Test
    fun `Attributed 결과로 귀속 성공 Decision을 생성한다`() {
        val partnerId = PartnerId()
        val result = AttributionResult.Attributed(partnerId)

        val decision = factory.create(result, conversionEvent, AttributionStrategy.LAST_CLICK)

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(decision.partnerId).isEqualTo(partnerId)
        assertThat(decision.conversionEventId).isEqualTo(conversionEvent.id)
    }

    @Test
    fun `Unattributed 결과로 귀속 실패 Decision을 생성한다`() {
        val result = AttributionResult.Unattributed

        val decision = factory.create(result, conversionEvent, AttributionStrategy.LAST_CLICK)

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
        assertThat(decision.partnerId).isNull()
    }
}
