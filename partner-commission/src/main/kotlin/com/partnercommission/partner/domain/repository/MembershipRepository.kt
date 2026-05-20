package com.partnercommission.partner.domain.repository

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.partner.domain.aggregate.Membership
import java.util.UUID

interface MembershipRepository {
    fun save(membership: Membership): Membership
    fun findById(id: UUID): Membership?
    fun findByPartnerId(partnerId: PartnerId): List<Membership>
}
