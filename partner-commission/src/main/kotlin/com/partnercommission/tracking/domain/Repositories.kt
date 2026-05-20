package com.partnercommission.tracking.domain

import java.util.UUID

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
