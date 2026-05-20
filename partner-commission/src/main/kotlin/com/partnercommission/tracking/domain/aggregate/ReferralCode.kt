package com.partnercommission.tracking.domain.aggregate

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

class ReferralCode private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val code: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    companion object {
        fun create(tenantId: TenantId, partnerId: PartnerId, code: String): ReferralCode = ReferralCode(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            partnerId = partnerId,
            code = code,
            createdAt = LocalDateTime.now(),
            expiresAt = null
        )

        fun reconstitute(
            id: UUID, tenantId: TenantId, partnerId: PartnerId,
            code: String, createdAt: LocalDateTime, expiresAt: LocalDateTime?
        ): ReferralCode = ReferralCode(id, tenantId, partnerId, code, createdAt, expiresAt)
    }
}
