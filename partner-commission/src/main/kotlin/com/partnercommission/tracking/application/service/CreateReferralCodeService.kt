package com.partnercommission.tracking.application.service

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.application.port.`in`.CreateReferralCodeUseCase
import com.partnercommission.tracking.domain.aggregate.ReferralCode
import com.partnercommission.tracking.domain.repository.ReferralCodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateReferralCodeService(private val referralCodeRepository: ReferralCodeRepository) : CreateReferralCodeUseCase {
    override fun execute(tenantId: TenantId, partnerId: PartnerId, code: String): ReferralCode {
        val referralCode = ReferralCode.create(tenantId, partnerId, code)
        return referralCodeRepository.save(referralCode)
    }
}
