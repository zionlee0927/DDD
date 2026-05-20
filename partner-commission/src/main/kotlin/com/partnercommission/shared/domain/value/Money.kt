package com.partnercommission.shared.domain.value

import java.math.BigDecimal

data class Money(val amount: BigDecimal, val currency: String = "KRW")
