package com.partnercommission.partner.infrastructure.outbound.persistence

import com.partnercommission.partner.domain.aggregate.Membership
import com.partnercommission.partner.domain.value.MembershipStatus
import com.partnercommission.partner.domain.value.Tier
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "memberships")
class MembershipJpaEntity(
    @Id val id: UUID,
    val partnerId: UUID,
    val tenantId: UUID,
    val tierLevel: String,
    val commissionRate: BigDecimal,
    @Enumerated(EnumType.STRING) val status: MembershipStatus,
    val joinedAt: LocalDateTime
) {
    fun toDomain(): Membership = Membership.reconstitute(
        id, PartnerId(partnerId), TenantId(tenantId),
        Tier(tierLevel, commissionRate), status, joinedAt
    )

    companion object {
        fun from(m: Membership) = MembershipJpaEntity(
            m.id, m.partnerId.value, m.tenantId.value,
            m.tier.level, m.tier.commissionRate, m.status, m.joinedAt
        )
    }
}
