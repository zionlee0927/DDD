package com.partnercommission.tracking.infrastructure.outbound.persistence

import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.repository.ClickRepository
import com.partnercommission.tracking.domain.repository.ReferralCodeRepository
import com.partnercommission.tracking.domain.repository.TrackingLinkRepository
import org.springframework.stereotype.Repository

@Repository
class TrackingLinkRepositoryImpl(private val jpa: TrackingLinkJpaRepository) : TrackingLinkRepository {
    override fun save(link: TrackingLink): TrackingLink = jpa.save(TrackingLinkJpaEntity.from(link)).toDomain()
    override fun findByTrackingCode(code: String): TrackingLink? = jpa.findByTrackingCode(code)?.toDomain()
}

@Repository
class ClickRepositoryImpl(private val jpa: ClickJpaRepository) : ClickRepository {
    override fun save(click: Click): Click = jpa.save(ClickJpaEntity.from(click)).toDomain()
}

@Repository
class ReferralCodeRepositoryImpl(private val jpa: ReferralCodeJpaRepository) : ReferralCodeRepository {
    override fun save(referralCode: ReferralCode): ReferralCode = jpa.save(ReferralCodeJpaEntity.from(referralCode)).toDomain()
    override fun findByCode(code: String): ReferralCode? = jpa.findByCode(code)?.toDomain()
}
