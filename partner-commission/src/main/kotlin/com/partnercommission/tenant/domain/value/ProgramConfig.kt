package com.partnercommission.tenant.domain.value

import java.time.Duration

enum class AttributionStrategyOption { LAST_CLICK, FIRST_CLICK }
enum class ConfirmationCondition { EVENT_BASED, TIME_BASED }

data class ProgramConfig(
    val attributionWindow: Duration = Duration.ofDays(30),
    val attributionStrategy: AttributionStrategyOption = AttributionStrategyOption.LAST_CLICK,
    val confirmationCondition: ConfirmationCondition = ConfirmationCondition.TIME_BASED,
    val confirmationDays: Int = 14
)
