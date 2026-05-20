package com.partnercommission.partner.domain.aggregate

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.partner.domain.value.MembershipStatus
import com.partnercommission.partner.domain.value.Tier
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class Membership private constructor(
    val id: UUID,
    val partnerId: PartnerId,
    val tenantId: TenantId,
    val tier: Tier,
    val status: MembershipStatus,
    val joinedAt: LocalDateTime
) {
    companion object {
        fun create(partnerId: PartnerId, tenantId: TenantId): Membership = Membership(
            id = UUID.randomUUID(),
            partnerId = partnerId,
            tenantId = tenantId,
            tier = Tier("BASIC", BigDecimal("0.05")),
            status = MembershipStatus.ACTIVE,
            joinedAt = LocalDateTime.now()
        )

        fun reconstitute(
            id: UUID, partnerId: PartnerId, tenantId: TenantId,
            tier: Tier, status: MembershipStatus, joinedAt: LocalDateTime
        ): Membership = Membership(id, partnerId, tenantId, tier, status, joinedAt)
    }
}
