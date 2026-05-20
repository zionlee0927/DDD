package com.partnercommission.attribution.application.service

import com.partnercommission.attribution.application.command.ReceiveConversionCommand
import com.partnercommission.attribution.application.port.`in`.ReceiveConversionUseCase
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.aggregate.ConversionEvent
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import com.partnercommission.attribution.domain.service.AttributionConfig
import com.partnercommission.attribution.domain.service.AttributionJudge
import com.partnercommission.attribution.domain.service.AttributionResult
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionStrategy
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.tenant.domain.repository.TenantReadRepository
import com.partnercommission.tracking.domain.repository.ClickReadRepository
import com.partnercommission.tracking.domain.repository.ReferralCodeReadRepository
import com.partnercommission.tracking.domain.repository.TrackingLinkReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class AttributionService(
    private val conversionEventRepository: ConversionEventRepository,
    private val attributionDecisionRepository: AttributionDecisionRepository,
    private val tenantRepository: TenantReadRepository,
    private val clickRepository: ClickReadRepository,
    private val trackingLinkRepository: TrackingLinkReadRepository,
    private val referralCodeRepository: ReferralCodeReadRepository,
) : ReceiveConversionUseCase {

    private val judge = AttributionJudge()

    override fun execute(command: ReceiveConversionCommand): AttributionDecision {
        // 1. 증거 결정
        val evidence = resolveEvidence(command)

        // 2. ConversionEvent 저장
        val conversionEvent = ConversionEvent.create(
            tenantId = command.tenantId,
            externalId = command.externalId,
            amount = Money(command.amount),
            eventType = command.eventType,
            evidence = evidence,
        )
        conversionEventRepository.save(conversionEvent)

        // 3. 테넌트 설정 조회 → AttributionConfig 변환 (ACL)
        val programConfig = tenantRepository.findProgramConfigById(command.tenantId)
            ?: throw IllegalArgumentException("Tenant not found: ${command.tenantId.value}")
        val config = AttributionConfig(
            attributionWindow = programConfig.attributionWindow,
            strategy = AttributionStrategy.valueOf(programConfig.attributionStrategy.name),
        )

        // 4. 증거 기반 데이터 조회
        val click = if (evidence?.type == EvidenceType.CLICK) {
            clickRepository.findById(UUID.fromString(evidence.referenceId))
        } else null

        val trackingLink = click?.let {
            trackingLinkRepository.findByTrackingCode(it.trackingCode)
        }

        val referralCode = if (evidence?.type == EvidenceType.REFERRAL_CODE) {
            referralCodeRepository.findByTenantAndCode(command.tenantId, evidence.referenceId)
        } else null

        // 5. 귀속 판정 (순수 Domain Service)
        val result = judge.judge(
            evidence = evidence,
            click = click,
            trackingLink = trackingLink,
            referralCode = referralCode,
            config = config,
        )

        // 6. AttributionDecision 생성 (이벤트는 Aggregate 내부에서 등록)
        val decision = when (result) {
            is AttributionResult.Attributed -> AttributionDecision.attributed(
                tenantId = command.tenantId,
                conversionEventId = conversionEvent.id,
                partnerId = result.partnerId,
                evidence = evidence!!,
                strategy = config.strategy,
                amount = conversionEvent.amount,
            )
            is AttributionResult.Unattributed -> AttributionDecision.unattributed(
                tenantId = command.tenantId,
                conversionEventId = conversionEvent.id,
                strategy = config.strategy,
            )
        }

        // 7. 저장
        attributionDecisionRepository.save(decision)

        return decision
    }

    private fun resolveEvidence(command: ReceiveConversionCommand): AttributionEvidence? {
        return when {
            command.clickId != null -> AttributionEvidence(EvidenceType.CLICK, command.clickId)
            command.referralCode != null -> AttributionEvidence(EvidenceType.REFERRAL_CODE, command.referralCode)
            else -> null
        }
    }
}
