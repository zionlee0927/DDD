package com.partnercommission.commission.domain.event

import com.partnercommission.commission.domain.value.CommissionId
import com.partnercommission.commission.domain.value.DeductionId
import com.partnercommission.shared.domain.DomainEvent
import com.partnercommission.shared.domain.value.Money
import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import java.time.LocalDateTime
import java.util.UUID

data class CommissionCalculated(
    val commissionId: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val amount: Money,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class CommissionCancelled(
    val commissionId: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class CommissionConfirmed(
    val commissionId: CommissionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val amount: Money,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent

data class DeductionCreated(
    val deductionId: DeductionId,
    val tenantId: TenantId,
    val partnerId: PartnerId,
    val originalCommissionId: CommissionId,
    val amount: Money,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
