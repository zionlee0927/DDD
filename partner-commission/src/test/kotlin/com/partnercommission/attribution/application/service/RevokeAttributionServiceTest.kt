package com.partnercommission.attribution.application.service

import com.partnercommission.attribution.application.port.`in`.RevokeAttributionCommand
import com.partnercommission.attribution.application.port.out.LoadTenantConfigPort
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.exception.AttributionNotFoundException
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import com.partnercommission.attribution.domain.value.*
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

class RevokeAttributionServiceTest {

    private val conversionEventRepository: ConversionEventRepository = mockk()
    private val attributionDecisionRepository: AttributionDecisionRepository = mockk()
    private val loadTenantConfigPort: LoadTenantConfigPort = mockk()

    private lateinit var service: RevokeAttributionService

    private val tenantId = TenantId(UUID.randomUUID())
    private val partnerId = PartnerId(UUID.randomUUID())
    private val externalId = "order-123"

    @BeforeEach
    fun setUp() {
        service = RevokeAttributionService(conversionEventRepository, attributionDecisionRepository, loadTenantConfigPort)
    }

    @Test
    fun `정상 철회`() {
        val conversionEvent = ConversionEvent.create(tenantId, externalId, Money(BigDecimal("50000")), "PAYMENT", AttributionEvidence(EvidenceType.CLICK, "click-1"))
        val decision = AttributionDecision.attributed(tenantId, conversionEvent.id, partnerId, AttributionEvidence(EvidenceType.CLICK, "click-1"), AttributionStrategy.LAST_CLICK, Money(BigDecimal("50000")))
        decision.getAndClearDomainEvents()

        every { conversionEventRepository.findByTenantAndExternalId(tenantId, externalId) } returns conversionEvent
        every { attributionDecisionRepository.findByConversionEventId(conversionEvent.id) } returns decision
        every { loadTenantConfigPort.loadConfig(tenantId) } returns AttributionConfig(Duration.ofDays(30), AttributionStrategy.LAST_CLICK, 30)
        val saved = slot<AttributionDecision>()
        every { attributionDecisionRepository.save(capture(saved)) } answers { saved.captured }

        val result = service.execute(RevokeAttributionCommand(tenantId, externalId))

        assertThat(result.currentStatus()).isEqualTo(AttributionStatus.REVOKED)
    }

    @Test
    fun `전환 없으면 예외`() {
        every { conversionEventRepository.findByTenantAndExternalId(tenantId, externalId) } returns null

        assertThatThrownBy { service.execute(RevokeAttributionCommand(tenantId, externalId)) }
            .isInstanceOf(AttributionNotFoundException::class.java)
    }

    @Test
    fun `귀속 판정 없으면 예외`() {
        val conversionEvent = ConversionEvent.create(tenantId, externalId, Money(BigDecimal("50000")), "PAYMENT", AttributionEvidence(EvidenceType.CLICK, "click-1"))
        every { conversionEventRepository.findByTenantAndExternalId(tenantId, externalId) } returns conversionEvent
        every { attributionDecisionRepository.findByConversionEventId(conversionEvent.id) } returns null

        assertThatThrownBy { service.execute(RevokeAttributionCommand(tenantId, externalId)) }
            .isInstanceOf(AttributionNotFoundException::class.java)
    }
}
