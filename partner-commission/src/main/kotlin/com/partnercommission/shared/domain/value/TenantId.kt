package com.partnercommission.shared.domain.value

import java.util.UUID

@JvmInline
value class TenantId(val value: UUID = UUID.randomUUID())
