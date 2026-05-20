package com.partnercommission.tenant.infrastructure.outbound.persistence

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.Tenant
import com.partnercommission.tenant.domain.value.*
import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tenants")
class TenantJpaEntity(
    @Id val id: UUID,
    val name: String,
    @Enumerated(EnumType.STRING) val status: TenantStatus,
    val attributionWindowMinutes: Long,
    @Enumerated(EnumType.STRING) val attributionStrategy: AttributionStrategy,
    @Enumerated(EnumType.STRING) val confirmationCondition: ConfirmationCondition,
    val confirmationDays: Int,
    val webhookUrl: String?,
    val webhookSecret: String?,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Tenant = Tenant.reconstitute(
        id = TenantId(id),
        name = name,
        status = status,
        programConfig = ProgramConfig(Duration.ofMinutes(attributionWindowMinutes), attributionStrategy, confirmationCondition, confirmationDays),
        webhookConfig = WebhookConfig(webhookUrl, webhookSecret),
        createdAt = createdAt
    )

    companion object {
        fun from(tenant: Tenant) = TenantJpaEntity(
            id = tenant.id.value,
            name = tenant.name,
            status = tenant.status,
            attributionWindowMinutes = tenant.programConfig.attributionWindow.toMinutes(),
            attributionStrategy = tenant.programConfig.attributionStrategy,
            confirmationCondition = tenant.programConfig.confirmationCondition,
            confirmationDays = tenant.programConfig.confirmationDays,
            webhookUrl = tenant.webhookConfig.url,
            webhookSecret = tenant.webhookConfig.secret,
            createdAt = tenant.createdAt
        )
    }
}
