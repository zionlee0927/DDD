package com.partnercommission.tracking.domain.aggregate

import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

class Click private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val trackingCode: String,
    val ipAddress: String,
    val userAgent: String,
    val clickedAt: LocalDateTime
) {
    companion object {
        fun create(tenantId: TenantId, trackingCode: String, ipAddress: String, userAgent: String): Click = Click(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            trackingCode = trackingCode,
            ipAddress = ipAddress,
            userAgent = userAgent,
            clickedAt = LocalDateTime.now()
        )

        fun reconstitute(
            id: UUID, tenantId: TenantId, trackingCode: String,
            ipAddress: String, userAgent: String, clickedAt: LocalDateTime
        ): Click = Click(id, tenantId, trackingCode, ipAddress, userAgent, clickedAt)
    }
}
