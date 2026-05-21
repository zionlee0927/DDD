package com.partnercommission.attribution.application.service

import com.partnercommission.attribution.application.port.`in`.ReceiveConversionCommand
import com.partnercommission.attribution.application.port.`in`.ReceiveConversionUseCase
import com.partnercommission.attribution.application.port.out.LoadTenantConfigPort
import com.partnercommission.attribution.application.service.processor.AttributeConversionProcessor
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.exception.DuplicateConversionException
import com.partnercommission.attribution.domain.exception.EvidenceRequiredException
import com.partnercommission.attribution.domain.exception.UnsupportedEvidenceTypeException
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import com.partnercommission.attribution.domain.service.AttributionDecisionFactory
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.shared.domain.value.Money
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReceiveConversionFacadeService(
    private val conversionEventRepository: ConversionEventRepository,
    private val attributionDecisionRepository: AttributionDecisionRepository,
    private val loadTenantConfigPort: LoadTenantConfigPort,
    private val attributeConversionProcessors: List<AttributeConversionProcessor>,
) : ReceiveConversionUseCase {

    private val decisionFactory = AttributionDecisionFactory()

    override fun execute(command: ReceiveConversionCommand): AttributionDecision {
        // === READ ===
        // 1. 중복 전환 체크 (멱등성)
        conversionEventRepository.findByTenantAndExternalId(command.tenantId, command.externalId)
            ?.let { throw DuplicateConversionException(command.externalId) }

        // 2. 테넌트 설정 조회
        val config = loadTenantConfigPort.loadConfig(command.tenantId)

        // === COMPUTE ===
        // 3. 증거 결정
        val evidence = AttributionEvidence.from(command.clickId, command.referralCode)
            ?: throw EvidenceRequiredException()

        // 4. ConversionEvent 생성
        val conversionEvent = ConversionEvent.create(
            tenantId = command.tenantId,
            externalId = command.externalId,
            amount = Money(command.amount),
            eventType = command.eventType,
            evidence = evidence,
        )

        // 5. 귀속 판정 (전략 패턴)
        val result = attributeConversionProcessors.firstOrNull { it.supports(evidence.type) }
            ?.attribute(evidence, command.tenantId, config)
            ?: throw UnsupportedEvidenceTypeException(evidence.type)

        // 6. AttributionDecision 생성
        val decision = decisionFactory.create(result, conversionEvent, config.strategy)

        // === WRITE ===
        // 7. 저장
        conversionEventRepository.save(conversionEvent)
        attributionDecisionRepository.save(decision)

        return decision
    }
}
