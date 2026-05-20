package com.partnercommission.tenant.infrastructure.outbound.persistence

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.ApiKey
import com.partnercommission.tenant.domain.value.ApiKeyStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "api_keys")
class ApiKeyJpaEntity(
    @Id val id: UUID,
    val tenantId: UUID,
    @Column(name = "api_key") val key: String,
    @Enumerated(EnumType.STRING) val status: ApiKeyStatus,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?
) {
    fun toDomain(): ApiKey = ApiKey.reconstitute(id, TenantId(tenantId), key, status, createdAt, expiresAt)

    companion object {
        fun from(apiKey: ApiKey) = ApiKeyJpaEntity(
            id = apiKey.id,
            tenantId = apiKey.tenantId.value,
            key = apiKey.key,
            status = apiKey.status,
            createdAt = apiKey.createdAt,
            expiresAt = apiKey.expiresAt
        )
    }
}
