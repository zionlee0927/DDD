package com.partnercommission.tracking.infrastructure.out.persistence

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.repository.ClickRepository
import com.partnercommission.tracking.domain.repository.ReferralCodeRepository
import com.partnercommission.tracking.domain.repository.TrackingLinkRepository
import com.partnercommission.tracking.domain.value.ClickView
import com.partnercommission.tracking.domain.value.ReferralCodeView
import com.partnercommission.tracking.domain.value.TrackingLinkView
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TrackingLinkRepositoryImpl(private val jpa: TrackingLinkJpaRepository) : TrackingLinkRepository {
    override fun save(link: TrackingLink): TrackingLink = jpa.save(TrackingLinkJpaEntity.from(link)).toDomain()
    override fun findByTrackingCode(code: String): TrackingLink? = jpa.findByTrackingCode(code)?.toDomain()
    override fun findViewByTrackingCode(code: String): TrackingLinkView? =
        jpa.findByTrackingCode(code)?.let { TrackingLinkView(partnerId = it.toDomain().partnerId) }
}

@Repository
class ClickRepositoryImpl(private val jpa: ClickJpaRepository) : ClickRepository {
    override fun save(click: Click): Click = jpa.save(ClickJpaEntity.from(click)).toDomain()
    override fun findById(id: UUID): Click? = jpa.findById(id).orElse(null)?.toDomain()
    override fun findViewById(id: UUID): ClickView? =
        jpa.findById(id).orElse(null)?.toDomain()?.let { ClickView(clickedAt = it.clickedAt, trackingCode = it.trackingCode) }
}

@Repository
class ReferralCodeRepositoryImpl(private val jpa: ReferralCodeJpaRepository) : ReferralCodeRepository {
    override fun save(referralCode: ReferralCode): ReferralCode = jpa.save(ReferralCodeJpaEntity.from(referralCode)).toDomain()
    override fun findByCode(code: String): ReferralCode? = jpa.findByCode(code)?.toDomain()
    override fun findViewByTenantAndCode(tenantId: TenantId, code: String): ReferralCodeView? =
        jpa.findByTenantIdAndCode(tenantId.value, code)?.toDomain()?.let { ReferralCodeView(partnerId = it.partnerId, expiresAt = it.expiresAt) }
}
