package com.partnercommission.partner.application

import com.partnercommission.partner.domain.Membership
import com.partnercommission.partner.domain.MembershipRepository
import com.partnercommission.partner.domain.PartnerRepository
import com.partnercommission.shared.domain.PartnerId
import com.partnercommission.shared.domain.TenantId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class JoinProgramUseCase(
    private val partnerRepository: PartnerRepository,
    private val membershipRepository: MembershipRepository
) {
    fun execute(partnerId: PartnerId, tenantId: TenantId): Membership {
        partnerRepository.findById(partnerId) ?: throw IllegalArgumentException("Partner not found")
        val membership = Membership.create(partnerId, tenantId)
        return membershipRepository.save(membership)
    }
}
