package com.partnercommission.partner.infrastructure.out.persistence

import com.partnercommission.partner.domain.aggregate.Membership
import com.partnercommission.partner.domain.repository.MembershipRepository
import com.partnercommission.shared.domain.value.PartnerId
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class MembershipRepositoryImpl(private val jpa: MembershipJpaRepository) : MembershipRepository {
    override fun save(membership: Membership): Membership = jpa.save(MembershipJpaEntity.from(membership)).toDomain()
    override fun findById(id: UUID): Membership? = jpa.findById(id).orElse(null)?.toDomain()
    override fun findByPartnerId(partnerId: PartnerId): List<Membership> = jpa.findByPartnerId(partnerId.value).map { it.toDomain() }
}
