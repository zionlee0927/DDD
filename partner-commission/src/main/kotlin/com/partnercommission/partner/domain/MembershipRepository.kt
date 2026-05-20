package com.partnercommission.partner.domain

import com.partnercommission.shared.domain.PartnerId
import java.util.UUID

interface MembershipRepository {
    fun save(membership: Membership): Membership
    fun findById(id: UUID): Membership?
    fun findByPartnerId(partnerId: PartnerId): List<Membership>
}
