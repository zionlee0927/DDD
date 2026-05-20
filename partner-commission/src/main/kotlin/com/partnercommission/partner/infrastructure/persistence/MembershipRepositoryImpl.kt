package com.partnercommission.partner.infrastructure.persistence

import com.partnercommission.partner.domain.Membership
import com.partnercommission.partner.domain.MembershipRepository
import com.partnercommission.shared.domain.PartnerId
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MembershipRepositoryImpl(private val jpa: MembershipJpaRepository) : MembershipRepository {
    override fun save(membership: Membership): Membership = jpa.save(MembershipJpaEntity.from(membership)).toDomain()
    override fun findById(id: UUID): Membership? = jpa.findById(id).orElse(null)?.toDomain()
    override fun findByPartnerId(partnerId: PartnerId): List<Membership> = jpa.findByPartnerId(partnerId.value).map { it.toDomain() }
}
