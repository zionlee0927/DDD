package com.partnercommission.partner.domain.repository

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.partner.domain.aggregate.Partner

interface PartnerRepository {
    fun save(partner: Partner): Partner
    fun findById(id: PartnerId): Partner?
}
