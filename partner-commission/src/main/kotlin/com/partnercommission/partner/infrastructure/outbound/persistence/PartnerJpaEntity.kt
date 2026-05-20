package com.partnercommission.partner.infrastructure.outbound.persistence

import com.partnercommission.partner.domain.aggregate.Partner
import com.partnercommission.partner.domain.value.PartnerStatus
import com.partnercommission.shared.domain.value.PartnerId
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "partners")
class PartnerJpaEntity(
    @Id val id: UUID,
    val name: String,
    val email: String,
    @Enumerated(EnumType.STRING) val status: PartnerStatus,
    val createdAt: LocalDateTime
) {
    fun toDomain(): Partner = Partner.reconstitute(PartnerId(id), name, email, status, createdAt)

    companion object {
        fun from(p: Partner) = PartnerJpaEntity(p.id.value, p.name, p.email, p.status, p.createdAt)
    }
}
