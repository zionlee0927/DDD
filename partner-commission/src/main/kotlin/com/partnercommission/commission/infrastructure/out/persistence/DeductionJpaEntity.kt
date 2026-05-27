package com.partnercommission.commission.infrastructure.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "deductions")
class DeductionJpaEntity(
    @Id val id: UUID,
    @Column(name = "tenant_id") val tenantId: UUID,
    @Column(name = "partner_id") val partnerId: UUID,
    @Column(name = "original_commission_id") val originalCommissionId: UUID,
    val amount: BigDecimal,
    val currency: String,
    @Column(name = "created_at") val createdAt: LocalDateTime,
)
