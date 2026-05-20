package com.partnercommission.tenant.domain

import com.partnercommission.shared.domain.TenantId
import java.time.LocalDateTime

class Tenant private constructor(
    val id: TenantId,
    val name: String,
    val status: TenantStatus,
    val programConfig: ProgramConfig,
    val webhookConfig: WebhookConfig,
    val createdAt: LocalDateTime
) {
    companion object {
        fun create(name: String): Tenant = Tenant(
            id = TenantId(),
            name = name,
            status = TenantStatus.ACTIVE,
            programConfig = ProgramConfig(),
            webhookConfig = WebhookConfig(),
            createdAt = LocalDateTime.now()
        )

        fun reconstitute(
            id: TenantId, name: String, status: TenantStatus,
            programConfig: ProgramConfig, webhookConfig: WebhookConfig, createdAt: LocalDateTime
        ): Tenant = Tenant(id, name, status, programConfig, webhookConfig, createdAt)
    }
}
