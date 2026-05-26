package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.HandleRevocationUseCase
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.value.CommissionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class HandleRevocationService(
    private val commissionRepository: CommissionRepository,
) : HandleRevocationUseCase {

    override fun execute(attributionDecisionId: String) {
        val commission = commissionRepository.findByAttributionDecisionId(attributionDecisionId)
            ?: throw CommissionNotFoundByAttributionException(attributionDecisionId)

        when (commission.currentStatus()) {
            CommissionStatus.PENDING -> {
                commission.cancel()
                commissionRepository.save(commission)
            }
            CommissionStatus.CONFIRMED, CommissionStatus.SETTLED -> {
                // TODO: E02-S04에서 Deduction 생성
            }
            CommissionStatus.CANCELLED -> { /* 이미 취소됨, 무시 */ }
        }
    }
}
