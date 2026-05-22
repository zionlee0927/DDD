package com.partnercommission.attribution.infrastructure.`in`.web

import com.partnercommission.attribution.application.port.`in`.ReceiveConversionCommand
import com.partnercommission.attribution.application.port.`in`.ReceiveConversionUseCase
import com.partnercommission.attribution.application.port.`in`.RevokeAttributionCommand
import com.partnercommission.attribution.application.port.`in`.RevokeAttributionUseCase
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class ReceiveConversionRequest(
    val tenantId: UUID,
    val externalId: String,
    val amount: BigDecimal,
    val eventType: String,
    val clickId: String?,
    val referralCode: String?,
)

data class AttributionDecisionResponse(
    val id: UUID,
    val status: String,
    val partnerId: UUID?,
    val strategy: String,
)

data class RevokeAttributionRequest(
    val tenantId: UUID,
    val externalId: String,
)

@RestController
@RequestMapping("/api/attributions")
class AttributionController(
    private val receiveConversionUseCase: ReceiveConversionUseCase,
    private val revokeAttributionUseCase: RevokeAttributionUseCase,
) {
    @PostMapping("/conversions")
    fun receiveConversion(@RequestBody request: ReceiveConversionRequest): ResponseEntity<AttributionDecisionResponse> {
        val command = ReceiveConversionCommand(
            tenantId = TenantId(request.tenantId),
            externalId = request.externalId,
            amount = request.amount,
            eventType = request.eventType,
            clickId = request.clickId,
            referralCode = request.referralCode,
        )

        val decision = receiveConversionUseCase.execute(command)

        return ResponseEntity.ok(
            AttributionDecisionResponse(
                id = decision.id.value,
                status = decision.currentStatus().name,
                partnerId = decision.partnerId?.value,
                strategy = decision.strategy.name,
            )
        )
    }

    @PostMapping("/revocations")
    fun revokeAttribution(@RequestBody request: RevokeAttributionRequest): ResponseEntity<AttributionDecisionResponse> {
        val command = RevokeAttributionCommand(
            tenantId = TenantId(request.tenantId),
            externalId = request.externalId,
        )

        val decision = revokeAttributionUseCase.execute(command)

        return ResponseEntity.ok(
            AttributionDecisionResponse(
                id = decision.id.value,
                status = decision.currentStatus().name,
                partnerId = decision.partnerId?.value,
                strategy = decision.strategy.name,
            )
        )
    }
}
