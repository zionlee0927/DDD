package com.partnercommission.attribution.domain.value

import com.partnercommission.shared.domain.value.PartnerId
import java.time.LocalDateTime

data class ClickData(
    val clickedAt: LocalDateTime,
    val trackingCode: String,
)

data class TrackingLinkData(
    val partnerId: PartnerId,
)

data class ReferralCodeData(
    val partnerId: PartnerId,
    val expiresAt: LocalDateTime?,
)
