package com.partnercommission.tenant.domain.value

import java.time.Duration

enum class AttributionStrategyOption { LAST_CLICK, FIRST_CLICK }
enum class ConfirmationCondition { EVENT_BASED, TIME_BASED }
enum class CommissionRuleType { PERCENTAGE, FIXED }

data class ProgramConfig(
    val attributionWindow: Duration = Duration.ofDays(30),
    val attributionStrategy: AttributionStrategyOption = AttributionStrategyOption.LAST_CLICK,
    val confirmationCondition: ConfirmationCondition = ConfirmationCondition.TIME_BASED,
    val confirmationDays: Int = 14,
    val commissionRuleType: CommissionRuleType = CommissionRuleType.PERCENTAGE,
    val commissionRuleValue: java.math.BigDecimal = java.math.BigDecimal("10"),
)
