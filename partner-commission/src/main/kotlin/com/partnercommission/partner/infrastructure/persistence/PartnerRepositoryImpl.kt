package com.partnercommission.partner.infrastructure.persistence

import com.partnercommission.partner.domain.Partner
import com.partnercommission.partner.domain.PartnerRepository
import com.partnercommission.shared.domain.PartnerId
import org.springframework.stereotype.Repository

@Repository
class PartnerRepositoryImpl(private val jpa: PartnerJpaRepository) : PartnerRepository {
    override fun save(partner: Partner): Partner = jpa.save(PartnerJpaEntity.from(partner)).toDomain()
    override fun findById(id: PartnerId): Partner? = jpa.findById(id.value).orElse(null)?.toDomain()
}
