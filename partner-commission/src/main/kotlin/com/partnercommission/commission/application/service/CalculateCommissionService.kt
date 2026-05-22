package com.partnercommission.commission.application.service

import com.partnercommission.commission.application.port.`in`.CalculateCommissionCommand
import com.partnercommission.commission.application.port.`in`.CalculateCommissionUseCase
import com.partnercommission.commission.application.port.out.LoadCommissionRulePort
import com.partnercommission.commission.domain.aggregate.Commission
import com.partnercommission.commission.domain.repository.CommissionRepository
import com.partnercommission.commission.domain.service.CommissionCalculator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CalculateCommissionService(
    private val commissionRepository: CommissionRepository,
    private val loadCommissionRulePort: LoadCommissionRulePort,
) : CalculateCommissionUseCase {

    private val calculator = CommissionCalculator()

    override fun execute(command: CalculateCommissionCommand): Commission {
        val rule = loadCommissionRulePort.loadRule(command.tenantId)
        val amount = calculator.calculate(command.conversionAmount, rule)

        val commission = Commission.create(
            tenantId = command.tenantId,
            partnerId = command.partnerId,
            attributionDecisionId = command.attributionDecisionId,
            amount = amount,
            rule = rule,
        )

        return commissionRepository.save(commission)
    }
}
