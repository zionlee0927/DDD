package com.partnercommission.tracking.application

import com.partnercommission.shared.domain.PartnerId
import com.partnercommission.shared.domain.TenantId
import com.partnercommission.tracking.domain.ReferralCode
import com.partnercommission.tracking.domain.ReferralCodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateReferralCodeUseCase(private val referralCodeRepository: ReferralCodeRepository) {
    fun execute(tenantId: TenantId, partnerId: PartnerId, code: String): ReferralCode {
        val referralCode = ReferralCode.create(tenantId, partnerId, code)
        return referralCodeRepository.save(referralCode)
    }
}
