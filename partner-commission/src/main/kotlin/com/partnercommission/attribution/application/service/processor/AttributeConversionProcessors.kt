package com.partnercommission.attribution.application.service.processor

import com.partnercommission.attribution.application.port.out.LoadClickPort
import com.partnercommission.attribution.application.port.out.LoadReferralCodePort
import com.partnercommission.attribution.domain.service.ClickAttributionJudge
import com.partnercommission.attribution.domain.service.ReferralCodeAttributionJudge
import com.partnercommission.attribution.domain.value.AttributionConfig
import com.partnercommission.attribution.domain.value.AttributionEvidence
import com.partnercommission.attribution.domain.value.AttributionResult
import com.partnercommission.attribution.domain.value.EvidenceType
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.stereotype.Component

interface AttributeConversionProcessor {
    fun supports(type: EvidenceType): Boolean
    fun attribute(evidence: AttributionEvidence, tenantId: TenantId, config: AttributionConfig): AttributionResult
}

@Component
class ClickAttributionProcessor(
    private val loadClickPort: LoadClickPort,
) : AttributeConversionProcessor {

    private val judge = ClickAttributionJudge()

    override fun supports(type: EvidenceType): Boolean = type == EvidenceType.CLICK

    override fun attribute(evidence: AttributionEvidence, tenantId: TenantId, config: AttributionConfig): AttributionResult {
        val clickData = loadClickPort.loadClick(evidence.referenceId)
            ?: return AttributionResult.Unattributed
        val trackingLinkData = loadClickPort.loadTrackingLink(clickData.trackingCode)
        return judge.judge(clickData, trackingLinkData, config)
    }
}

@Component
class ReferralCodeAttributionProcessor(
    private val loadReferralCodePort: LoadReferralCodePort,
) : AttributeConversionProcessor {

    private val judge = ReferralCodeAttributionJudge()

    override fun supports(type: EvidenceType): Boolean = type == EvidenceType.REFERRAL_CODE

    override fun attribute(evidence: AttributionEvidence, tenantId: TenantId, config: AttributionConfig): AttributionResult {
        val referralCodeData = loadReferralCodePort.loadReferralCode(tenantId, evidence.referenceId)
        return judge.judge(referralCodeData)
    }
}
