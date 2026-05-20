package com.partnercommission.partner.infrastructure

import com.partnercommission.partner.application.JoinProgramUseCase
import com.partnercommission.partner.application.RegisterPartnerUseCase
import com.partnercommission.partner.domain.PartnerRepository
import com.partnercommission.shared.domain.PartnerId
import com.partnercommission.shared.domain.TenantId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/partners")
class PartnerController(
    private val registerPartner: RegisterPartnerUseCase,
    private val joinProgram: JoinProgramUseCase,
    private val partnerRepository: PartnerRepository
) {
    data class CreatePartnerRequest(val name: String, val email: String)
    data class JoinProgramRequest(val tenantId: UUID)
    data class PartnerResponse(val id: UUID, val name: String, val email: String, val status: String)
    data class MembershipResponse(val id: UUID, val tenantId: UUID, val tier: String, val status: String)

    @PostMapping
    fun create(@RequestBody req: CreatePartnerRequest): ResponseEntity<PartnerResponse> {
        val partner = registerPartner.execute(req.name, req.email)
        return ResponseEntity.ok(PartnerResponse(partner.id.value, partner.name, partner.email, partner.status.name))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<PartnerResponse> {
        val partner = partnerRepository.findById(PartnerId(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(PartnerResponse(partner.id.value, partner.name, partner.email, partner.status.name))
    }

    @PostMapping("/{id}/memberships")
    fun join(@PathVariable id: UUID, @RequestBody req: JoinProgramRequest): ResponseEntity<MembershipResponse> {
        val membership = joinProgram.execute(PartnerId(id), TenantId(req.tenantId))
        return ResponseEntity.ok(MembershipResponse(membership.id, membership.tenantId.value, membership.tier.level, membership.status.name))
    }
}
