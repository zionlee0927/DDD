package com.partnercommission.commission.domain.service

import com.partnercommission.commission.domain.value.CommissionStatus

enum class RevocationAction { CANCEL, DEDUCT, IGNORE }

class RevocationPolicy {
    fun decide(status: CommissionStatus): RevocationAction {
        return when (status) {
            CommissionStatus.PENDING -> RevocationAction.CANCEL
            CommissionStatus.CONFIRMED, CommissionStatus.SETTLED -> RevocationAction.DEDUCT
            CommissionStatus.CANCELLED -> RevocationAction.IGNORE
        }
    }
}
