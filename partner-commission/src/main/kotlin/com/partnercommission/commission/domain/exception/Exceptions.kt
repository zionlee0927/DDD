package com.partnercommission.commission.domain.exception

import com.partnercommission.commission.domain.value.CommissionId

sealed class CommissionException(message: String) : RuntimeException(message)

class CommissionNotFoundException(id: CommissionId) :
    CommissionException("Commission not found: ${id.value}")

class CommissionNotFoundByAttributionException(attributionDecisionId: String) :
    CommissionException("Commission not found for attribution: $attributionDecisionId")
