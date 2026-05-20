package com.partnercommission.attribution.domain.value

import java.util.UUID

@JvmInline
value class AttributionDecisionId(val value: UUID) {
    companion object {
        fun generate(): AttributionDecisionId = AttributionDecisionId(UUID.randomUUID())
        fun of(value: String): AttributionDecisionId = AttributionDecisionId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@JvmInline
value class ConversionEventId(val value: UUID) {
    companion object {
        fun generate(): ConversionEventId = ConversionEventId(UUID.randomUUID())
        fun of(value: String): ConversionEventId = ConversionEventId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
