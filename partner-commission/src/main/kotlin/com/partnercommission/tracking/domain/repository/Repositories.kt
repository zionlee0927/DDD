package com.partnercommission.tracking.domain.repository

import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.aggregate.TrackingLink

interface TrackingLinkRepository {
    fun save(link: TrackingLink): TrackingLink
    fun findByTrackingCode(code: String): TrackingLink?
}

interface ClickRepository {
    fun save(click: Click): Click
}

interface ReferralCodeRepository {
    fun save(referralCode: ReferralCode): ReferralCode
    fun findByCode(code: String): ReferralCode?
}
