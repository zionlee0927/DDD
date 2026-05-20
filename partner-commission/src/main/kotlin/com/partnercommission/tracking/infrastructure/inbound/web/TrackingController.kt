package com.partnercommission.tracking.infrastructure.inbound.web

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.application.port.`in`.CreateReferralCodeUseCase
import com.partnercommission.tracking.application.port.`in`.CreateTrackingLinkUseCase
import com.partnercommission.tracking.application.port.`in`.RecordClickUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class TrackingController(
    private val createTrackingLink: CreateTrackingLinkUseCase,
    private val recordClick: RecordClickUseCase,
    private val createReferralCode: CreateReferralCodeUseCase
) {
    data class CreateTrackingLinkRequest(val tenantId: UUID, val partnerId: UUID, val targetUrl: String)
    data class TrackingLinkResponse(val id: UUID, val trackingCode: String, val targetUrl: String)
    data class CreateReferralCodeRequest(val tenantId: UUID, val partnerId: UUID, val code: String)
    data class ReferralCodeResponse(val id: UUID, val code: String)

    @PostMapping("/api/tracking-links")
    fun create(@RequestBody req: CreateTrackingLinkRequest): ResponseEntity<TrackingLinkResponse> {
        val link = createTrackingLink.execute(TenantId(req.tenantId), PartnerId(req.partnerId), req.targetUrl)
        return ResponseEntity.ok(TrackingLinkResponse(link.id, link.trackingCode, link.targetUrl))
    }

    @GetMapping("/api/redirect/{trackingCode}")
    fun redirect(@PathVariable trackingCode: String, request: HttpServletRequest): ResponseEntity<Void> {
        val link = recordClick.execute(
            trackingCode,
            request.remoteAddr ?: "unknown",
            request.getHeader("User-Agent") ?: "unknown"
        )
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, link.targetUrl).build()
    }

    @PostMapping("/api/referral-codes")
    fun createReferral(@RequestBody req: CreateReferralCodeRequest): ResponseEntity<ReferralCodeResponse> {
        val code = createReferralCode.execute(TenantId(req.tenantId), PartnerId(req.partnerId), req.code)
        return ResponseEntity.ok(ReferralCodeResponse(code.id, code.code))
    }
}
