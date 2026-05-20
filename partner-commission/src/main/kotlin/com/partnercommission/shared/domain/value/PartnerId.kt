package com.partnercommission.shared.domain.value

import java.util.UUID

@JvmInline
value class PartnerId(val value: UUID = UUID.randomUUID())
