package com.partnercommission.shared.domain

import java.util.UUID

@JvmInline
value class PartnerId(val value: UUID = UUID.randomUUID())
