package com.partnercommission.commission.infrastructure.`in`.web

import com.partnercommission.commission.application.port.`in`.ConfirmCommissionCommand
import com.partnercommission.commission.application.port.`in`.ConfirmCommissionUseCase
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class ConfirmCommissionRequest(
    val tenantId: UUID,
    val attributionDecisionId: String,
)

data class CommissionResponse(
    val id: UUID,
    val status: String,
    val amount: BigDecimal,
)

@RestController
@RequestMapping("/api/commissions")
class CommissionController(
    private val confirmCommissionUseCase: ConfirmCommissionUseCase,
) {
    @PostMapping("/confirmations")
    fun confirm(@RequestBody request: ConfirmCommissionRequest): ResponseEntity<CommissionResponse> {
        val commission = confirmCommissionUseCase.execute(
            ConfirmCommissionCommand(
                tenantId = TenantId(request.tenantId),
                attributionDecisionId = request.attributionDecisionId,
            )
        )
        return ResponseEntity.ok(
            CommissionResponse(
                id = commission.id.value,
                status = commission.currentStatus().name,
                amount = commission.amount.amount,
            )
        )
    }
}
