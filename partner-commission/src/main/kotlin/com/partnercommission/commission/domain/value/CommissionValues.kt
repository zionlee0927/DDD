package com.partnercommission.commission.domain.value

import java.math.BigDecimal
import java.util.UUID

@JvmInline
value class CommissionId(val value: UUID) {
    companion object {
        fun generate(): CommissionId = CommissionId(UUID.randomUUID())
        fun of(value: String): CommissionId = CommissionId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

enum class CommissionStatus { PENDING, CONFIRMED, SETTLED, CANCELLED }
enum class RuleType { PERCENTAGE, FIXED }

data class CommissionRule(val type: RuleType, val value: BigDecimal) {
    init {
        require(value > BigDecimal.ZERO) { "커미션 규칙 값은 0보다 커야 한다" }
    }
}
