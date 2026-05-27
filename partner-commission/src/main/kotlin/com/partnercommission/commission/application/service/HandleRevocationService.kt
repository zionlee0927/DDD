package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.HandleRevocationUseCase
import com.partnercommission.commission.domain.aggregate.Deduction
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.repository.DeductionRepository
import com.partnercommission.commission.domain.service.RevocationAction
import com.partnercommission.commission.domain.service.RevocationPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HandleRevocationService(
    private val commissionRepository: CommissionRepository,
    private val deductionRepository: DeductionRepository,
) : HandleRevocationUseCase {

    private val revocationPolicy = RevocationPolicy()

    override fun execute(attributionDecisionId: String) {
        val commission = commissionRepository.findByAttributionDecisionId(attributionDecisionId)
            ?: throw CommissionNotFoundByAttributionException(attributionDecisionId)

        when (revocationPolicy.decide(commission.currentStatus())) {
            RevocationAction.CANCEL -> {
                commission.cancel()
                commissionRepository.save(commission)
            }
            RevocationAction.DEDUCT -> {
                if (deductionRepository.findByOriginalCommissionId(commission.id) != null) return
                val deduction = Deduction.create(
                    tenantId = commission.tenantId,
                    partnerId = commission.partnerId,
                    originalCommissionId = commission.id,
                    amount = commission.amount,
                )
                deductionRepository.save(deduction)
            }
            RevocationAction.IGNORE -> {}
        }
    }
}
