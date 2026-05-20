package com.partnercommission.partner.application.service

import com.partnercommission.partner.application.port.`in`.JoinProgramUseCase
import com.partnercommission.partner.domain.aggregate.Membership
import com.partnercommission.partner.domain.repository.MembershipRepository
import com.partnercommission.partner.domain.repository.PartnerRepository
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class JoinProgramService(
    private val partnerRepository: PartnerRepository,
    private val membershipRepository: MembershipRepository
) : JoinProgramUseCase {
    override fun execute(partnerId: PartnerId, tenantId: TenantId): Membership {
        partnerRepository.findById(partnerId) ?: throw IllegalArgumentException("Partner not found")
        val membership = Membership.create(partnerId, tenantId)
        return membershipRepository.save(membership)
    }
}
