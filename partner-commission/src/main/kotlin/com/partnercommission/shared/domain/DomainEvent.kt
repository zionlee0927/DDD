package com.partnercommission.shared.domain

import java.time.LocalDateTime
import java.util.UUID

interface DomainEvent {
    val eventId: UUID
    val occurredAt: LocalDateTime
}
