package com.partnercommission.partner.application.port.`in`

import com.partnercommission.partner.domain.aggregate.Partner

interface RegisterPartnerUseCase {
    fun execute(name: String, email: String): Partner
}
