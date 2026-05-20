package com.partnercommission.tracking.infrastructure.outbound.persistence

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tracking_links")
class TrackingLinkJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val partnerId: UUID,
    val targetUrl: String,
    @Column(unique = true) val trackingCode: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    fun toDomain(): TrackingLink = TrackingLink.reconstitute(
        id, TenantId(tenantId), PartnerId(partnerId), targetUrl, trackingCode, createdAt, expiresAt
    )

    companion object {
        fun from(l: TrackingLink) = TrackingLinkJpaEntity(
            l.id, l.tenantId.value, l.partnerId.value, l.targetUrl, l.trackingCode, l.createdAt, l.expiresAt
        )
    }
}

@Entity
@Table(name = "clicks")
class ClickJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val trackingCode: String,
    val ipAddress: String,
    val userAgent: String,
    val clickedAt: LocalDateTime
) {
    fun toDomain(): Click = Click.reconstitute(id, TenantId(tenantId), trackingCode, ipAddress, userAgent, clickedAt)

    companion object {
        fun from(c: Click) = ClickJpaEntity(c.id, c.tenantId.value, c.trackingCode, c.ipAddress, c.userAgent, c.clickedAt)
    }
}

@Entity
@Table(name = "referral_codes")
class ReferralCodeJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    val partnerId: UUID,
    @Column(unique = true) val code: String,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    fun toDomain(): ReferralCode = ReferralCode.reconstitute(
        id, TenantId(tenantId), PartnerId(partnerId), code, createdAt, expiresAt
    )

    companion object {
        fun from(r: ReferralCode) = ReferralCodeJpaEntity(
            r.id, r.tenantId.value, r.partnerId.value, r.code, r.createdAt, r.expiresAt
        )
    }
}
