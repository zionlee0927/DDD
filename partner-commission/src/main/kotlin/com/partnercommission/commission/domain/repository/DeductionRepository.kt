package com.partnercommission.commission.domain.repository

import com.partnercommission.commission.domain.aggregate.Deduction
import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.DeductionId

interface DeductionRepository {
    fun save(deduction: Deduction): Deduction
    fun findById(id: DeductionId): Deduction?
    fun findByOriginalCommissionId(commissionId: CommissionId): Deduction?
}
