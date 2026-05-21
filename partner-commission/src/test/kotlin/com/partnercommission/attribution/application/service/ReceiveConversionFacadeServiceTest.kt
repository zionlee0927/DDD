package com.partnercommission.attribution.application.service

import com.partnercommission.attribution.application.port.`in`.ReceiveConversionCommand
import com.partnercommission.attribution.application.port.out.LoadTenantConfigPort
import com.partnercommission.attribution.application.service.processor.AttributeConversionProcessor
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.exception.DuplicateConversionException
import com.partnercommission.attribution.domain.exception.EvidenceRequiredException
import com.partnercommission.attribution.domain.exception.UnsupportedEvidenceTypeException
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class ReceiveConversionFacadeServiceTest {

    private val conversionEventRepository: ConversionEventRepository = mockk()
    private val attributionDecisionRepository: AttributionDecisionRepository = mockk()
    private val loadTenantConfigPort: LoadTenantConfigPort = mockk()
    private val clickProcessor: AttributeConversionProcessor = mockk()

    private lateinit var service: ReceiveConversionFacadeService

    private val tenantId = TenantId()
    private val config = AttributionConfig(
        attributionWindow = Duration.ofDays(30),
        strategy = AttributionStrategy.LAST_CLICK,
    )

    @BeforeEach
    fun setUp() {
        service = ReceiveConversionFacadeService(
            conversionEventRepository = conversionEventRepository,
            attributionDecisionRepository = attributionDecisionRepository,
            loadTenantConfigPort = loadTenantConfigPort,
            attributeConversionProcessors = listOf(clickProcessor),
        )

        every { clickProcessor.supports(EvidenceType.CLICK) } returns true
        every { clickProcessor.supports(EvidenceType.REFERRAL_CODE) } returns false
        every { conversionEventRepository.findByTenantAndExternalId(any(), any()) } returns null
        every { loadTenantConfigPort.loadConfig(any()) } returns config
        every { conversionEventRepository.save(any()) } answers { firstArg() }
        every { attributionDecisionRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `귀속 성공 시 ATTRIBUTED Decision을 반환한다`() {
        val partnerId = PartnerId()
        every { clickProcessor.attribute(any(), any(), any()) } returns AttributionResult.Attributed(partnerId)

        val command = ReceiveConversionCommand(
            tenantId = tenantId,
            externalId = "order-1",
            amount = BigDecimal("50000"),
            eventType = "PAYMENT",
            clickId = "click-123",
            referralCode = null,
        )

        val decision = service.execute(command)

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.ATTRIBUTED)
        assertThat(decision.partnerId).isEqualTo(partnerId)
        verify { conversionEventRepository.save(any()) }
        verify { attributionDecisionRepository.save(any()) }
    }

    @Test
    fun `귀속 실패 시 UNATTRIBUTED Decision을 반환한다`() {
        every { clickProcessor.attribute(any(), any(), any()) } returns AttributionResult.Unattributed

        val command = ReceiveConversionCommand(
            tenantId = tenantId,
            externalId = "order-2",
            amount = BigDecimal("30000"),
            eventType = "PAYMENT",
            clickId = "click-456",
            referralCode = null,
        )

        val decision = service.execute(command)

        assertThat(decision.currentStatus()).isEqualTo(AttributionStatus.UNATTRIBUTED)
        assertThat(decision.partnerId).isNull()
    }

    @Test
    fun `중복 전환이면 DuplicateConversionException`() {
        every { conversionEventRepository.findByTenantAndExternalId(any(), any()) } returns mockk<ConversionEvent>()

        val command = ReceiveConversionCommand(
            tenantId = tenantId,
            externalId = "order-dup",
            amount = BigDecimal("10000"),
            eventType = "PAYMENT",
            clickId = "click-1",
            referralCode = null,
        )

        assertThatThrownBy { service.execute(command) }
            .isInstanceOf(DuplicateConversionException::class.java)
    }

    @Test
    fun `증거가 없으면 EvidenceRequiredException`() {
        val command = ReceiveConversionCommand(
            tenantId = tenantId,
            externalId = "order-3",
            amount = BigDecimal("10000"),
            eventType = "PAYMENT",
            clickId = null,
            referralCode = null,
        )

        assertThatThrownBy { service.execute(command) }
            .isInstanceOf(EvidenceRequiredException::class.java)
    }

    @Test
    fun `지원하지 않는 증거 타입이면 UnsupportedEvidenceTypeException`() {
        val command = ReceiveConversionCommand(
            tenantId = tenantId,
            externalId = "order-4",
            amount = BigDecimal("10000"),
            eventType = "PAYMENT",
            clickId = null,
            referralCode = "CODE1",
        )

        assertThatThrownBy { service.execute(command) }
            .isInstanceOf(UnsupportedEvidenceTypeException::class.java)
    }
}
