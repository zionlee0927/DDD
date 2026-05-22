package com.partnercommission.integration.framework

import com.partnercommission.attribution.application.port.`in`.ReceiveConversionCommand
import com.partnercommission.attribution.application.port.`in`.ReceiveConversionUseCase
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.application.port.`in`.CreateTrackingLinkUseCase
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.repository.ClickRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class AttributionScenarioContext(
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val externalId: String = "order-${System.nanoTime()}",
    val amount: BigDecimal = BigDecimal("50000"),
    val eventType: String = "PAYMENT",
    val targetUrl: String = "https://example.com",
)

data class AttributionFlowResult(
    val trackingLink: TrackingLink? = null,
    val click: Click? = null,
    val decision: AttributionDecision? = null,
)

@Component
class AttributionFlowExecutor(
    private val createTrackingLink: CreateTrackingLinkUseCase,
    private val receiveConversion: ReceiveConversionUseCase,
    private val clickRepository: ClickRepository,
) {
    /** 전체 흐름: 링크 발급 → 클릭 → 전환 수신 */
    fun executeFullFlow(ctx: AttributionScenarioContext): AttributionFlowResult {
        val link = executeCreateLink(ctx)
        val click = executeRecordClick(link)
        val decision = executeConversion(ctx, clickId = click.id.toString())
        return AttributionFlowResult(link, click, decision)
    }

    /** 클릭까지만 */
    fun executeUntilClick(ctx: AttributionScenarioContext): AttributionFlowResult {
        val link = executeCreateLink(ctx)
        val click = executeRecordClick(link)
        return AttributionFlowResult(link, click)
    }

    /** 전환 수신만 (클릭 ID 직접 전달) */
    fun executeConversionWithClick(ctx: AttributionScenarioContext, clickId: String): AttributionFlowResult {
        val decision = executeConversion(ctx, clickId = clickId)
        return AttributionFlowResult(decision = decision)
    }

    /** 추천코드 기반 전환 */
    fun executeConversionWithReferralCode(ctx: AttributionScenarioContext, referralCode: String): AttributionFlowResult {
        val decision = executeConversion(ctx, referralCode = referralCode)
        return AttributionFlowResult(decision = decision)
    }

    private fun executeCreateLink(ctx: AttributionScenarioContext): TrackingLink {
        return createTrackingLink.execute(ctx.tenantId, ctx.partnerId, ctx.targetUrl)
    }

    private fun executeRecordClick(link: TrackingLink): Click {
        val click = Click.create(link.tenantId, link.trackingCode, "127.0.0.1", "E2E-Test-Agent")
        return clickRepository.save(click)
    }

    private fun executeConversion(
        ctx: AttributionScenarioContext,
        clickId: String? = null,
        referralCode: String? = null,
    ): AttributionDecision {
        return receiveConversion.execute(
            ReceiveConversionCommand(
                tenantId = ctx.tenantId,
                externalId = ctx.externalId,
                amount = ctx.amount,
                eventType = ctx.eventType,
                clickId = clickId,
                referralCode = referralCode,
            )
        )
    }
}
