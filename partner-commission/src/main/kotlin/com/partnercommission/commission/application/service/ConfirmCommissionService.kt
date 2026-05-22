package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.ConfirmCommissionCommand
import com.partnercommission.commission.application.port.`in`.ConfirmCommissionUseCase
import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import com.partnercommission.commission.domain.repository.CommissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ConfirmCommissionService(
    private val commissionRepository: CommissionRepository,
) : ConfirmCommissionUseCase {

    override fun execute(command: ConfirmCommissionCommand): Commission {
        val commission = commissionRepository.findByAttributionDecisionId(command.attributionDecisionId)
            ?: throw CommissionNotFoundByAttributionException(command.attributionDecisionId)
        commission.confirm()
        return commissionRepository.save(commission)
    }
}
