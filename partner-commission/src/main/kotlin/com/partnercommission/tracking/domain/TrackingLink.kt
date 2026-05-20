package com.partnercommission.tracking.domain

import com.partnercommission.shared.domain.PartnerId
import com.partnercommission.shared.domain.TenantId
import java.time.LocalDateTime
import java.util.UUID

class TrackingLink private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val targetUrl: String,
    val trackingCode: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    companion object {
        fun create(tenantId: TenantId, partnerId: PartnerId, targetUrl: String): TrackingLink = TrackingLink(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            partnerId = partnerId,
            targetUrl = targetUrl,
            trackingCode = UUID.randomUUID().toString().take(8),
            createdAt = LocalDateTime.now(),
            expiresAt = null
        )

        fun reconstitute(
            id: UUID, tenantId: TenantId, partnerId: PartnerId,
            targetUrl: String, trackingCode: String, createdAt: LocalDateTime, expiresAt: LocalDateTime?
        ): TrackingLink = TrackingLink(id, tenantId, partnerId, targetUrl, trackingCode, createdAt, expiresAt)
    }
}
