package com.partnercommission.tracking.infrastructure.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TrackingLinkJpaRepository : JpaRepository<TrackingLinkJpaEntity, UUID> {
    fun findByTrackingCode(code: String): TrackingLinkJpaEntity?
}

interface ClickJpaRepository : JpaRepository<ClickJpaEntity, UUID>

interface ReferralCodeJpaRepository : JpaRepository<ReferralCodeJpaEntity, UUID> {
    fun findByCode(code: String): ReferralCodeJpaEntity?
}
