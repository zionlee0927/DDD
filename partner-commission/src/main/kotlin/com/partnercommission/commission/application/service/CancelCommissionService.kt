package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.CancelCommissionUseCase
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import com.partnercommission.commission.domain.repository.CommissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelCommissionService(
    private val commissionRepository: CommissionRepository,
) : CancelCommissionUseCase {

    override fun execute(attributionDecisionId: String) {
        val commission = commissionRepository.findByAttributionDecisionId(attributionDecisionId)
            ?: throw CommissionNotFoundByAttributionException(attributionDecisionId)
        commission.cancel()
        commissionRepository.save(commission)
    }
}
