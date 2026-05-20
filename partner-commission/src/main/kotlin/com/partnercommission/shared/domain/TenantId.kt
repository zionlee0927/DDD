package com.partnercommission.shared.domain

import java.util.UUID

@JvmInline
value class TenantId(val value: UUID = UUID.randomUUID())
