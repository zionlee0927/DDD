package com.partnercommission.partner.domain.aggregate

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.partner.domain.value.PartnerStatus
import java.time.LocalDateTime

class Partner private constructor(
    val id: PartnerId,
    val name: String,
    val email: String,
    val status: PartnerStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun create(name: String, email: String): Partner = Partner(
            id = PartnerId(),
            name = name,
            email = email,
            status = PartnerStatus.ACTIVE,
            createdAt = LocalDateTime.now()
        )

        fun reconstitute(id: PartnerId, name: String, email: String, status: PartnerStatus, createdAt: LocalDateTime): Partner =
            Partner(id, name, email, status, createdAt)
    }
}
