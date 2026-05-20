package com.partnercommission.tenant.domain.aggregate

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.value.ApiKeyStatus
import java.time.LocalDateTime
import java.util.UUID

class ApiKey private constructor(
    val id: UUID,
    val tenantId: TenantId,
    val key: String,
    val status: ApiKeyStatus,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    companion object {
        fun create(tenantId: TenantId): ApiKey = ApiKey(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            key = UUID.randomUUID().toString().replace("-", ""),
            status = ApiKeyStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            expiresAt = null
        )

        fun reconstitute(
            id: UUID, tenantId: TenantId, key: String,
            status: ApiKeyStatus, createdAt: LocalDateTime, expiresAt: LocalDateTime?
        ): ApiKey = ApiKey(id, tenantId, key, status, createdAt, expiresAt)
    }
}
