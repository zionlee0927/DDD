package com.partnercommission.partner.domain

import com.partnercommission.shared.domain.PartnerId

interface PartnerRepository {
    fun save(partner: Partner): Partner
    fun findById(id: PartnerId): Partner?
}
