package com.partnercommission.partner.domain

import java.math.BigDecimal

enum class PartnerStatus { ACTIVE, INACTIVE }
enum class MembershipStatus { ACTIVE, SUSPENDED }

data class Tier(val level: String, val commissionRate: BigDecimal)
