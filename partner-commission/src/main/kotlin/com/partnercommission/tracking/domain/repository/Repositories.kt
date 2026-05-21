package com.partnercommission.tracking.domain.repository

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.value.ClickView
import com.partnercommission.tracking.domain.value.ReferralCodeView
import com.partnercommission.tracking.domain.value.TrackingLinkView
import java.util.UUID

// ==================== Read Repositories (타 BC 노출용) ====================

interface TrackingLinkReadRepository {
    fun findViewByTrackingCode(code: String): TrackingLinkView?
}

interface ClickReadRepository {
    fun findViewById(id: UUID): ClickView?
}

interface ReferralCodeReadRepository {
    fun findViewByTenantAndCode(tenantId: TenantId, code: String): ReferralCodeView?
}

// ==================== Full Repositories (자기 BC 전용) ====================

interface TrackingLinkRepository : TrackingLinkReadRepository {
    fun findByTrackingCode(code: String): TrackingLink?
    fun save(link: TrackingLink): TrackingLink
}

interface ClickRepository : ClickReadRepository {
    fun findById(id: UUID): Click?
    fun save(click: Click): Click
}

interface ReferralCodeRepository : ReferralCodeReadRepository {
    fun findByCode(code: String): ReferralCode?
    fun save(referralCode: ReferralCode): ReferralCode
}
