package com.partnercommission.tracking.domain.value

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime

data class ClickView(
    val clickedAt: LocalDateTime,
    val trackingCode: String,
)

data class TrackingLinkView(
    val partnerId: PartnerId,
)

data class ReferralCodeView(
    val partnerId: PartnerId,
    val expiresAt: LocalDateTime?,
)
