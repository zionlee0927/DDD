package com.partnercommission.tracking.domain.repository

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import java.util.UUID

// ==================== Read Repositories (타 BC 노출용) ====================

interface TrackingLinkReadRepository {
    fun findByTrackingCode(code: String): TrackingLink?
}

interface ClickReadRepository {
    fun findById(id: UUID): Click?
}

interface ReferralCodeReadRepository {
    fun findByTenantAndCode(tenantId: TenantId, code: String): ReferralCode?
    fun findByCode(code: String): ReferralCode?
}

// ==================== Full Repositories (자기 BC 전용) ====================

interface TrackingLinkRepository : TrackingLinkReadRepository {
    fun save(link: TrackingLink): TrackingLink
}

interface ClickRepository : ClickReadRepository {
    fun save(click: Click): Click
}

interface ReferralCodeRepository : ReferralCodeReadRepository {
    fun save(referralCode: ReferralCode): ReferralCode
}
