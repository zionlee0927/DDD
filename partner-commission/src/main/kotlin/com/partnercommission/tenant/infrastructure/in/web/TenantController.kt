package com.partnercommission.tenant.infrastructure.`in`.web

import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.application.port.`in`.IssueApiKeyUseCase
import com.partnercommission.tenant.application.port.`in`.RegisterTenantUseCase
import com.partnercommission.tenant.domain.repository.TenantRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/tenants")
class TenantController(
    private val registerTenant: RegisterTenantUseCase,
    private val issueApiKey: IssueApiKeyUseCase,
    private val tenantRepository: TenantRepository
) {
    data class CreateTenantRequest(val name: String)
    data class TenantResponse(val id: UUID, val name: String, val status: String)
    data class ApiKeyResponse(val id: UUID, val key: String, val status: String)

    @PostMapping
    fun create(@RequestBody req: CreateTenantRequest): ResponseEntity<TenantResponse> {
        val tenant = registerTenant.execute(req.name)
        return ResponseEntity.ok(TenantResponse(tenant.id.value, tenant.name, tenant.status.name))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<TenantResponse> {
        val tenant = tenantRepository.findById(TenantId(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(TenantResponse(tenant.id.value, tenant.name, tenant.status.name))
    }

    @PostMapping("/{id}/api-keys")
    fun issueKey(@PathVariable id: UUID): ResponseEntity<ApiKeyResponse> {
        val apiKey = issueApiKey.execute(TenantId(id))
        return ResponseEntity.ok(ApiKeyResponse(apiKey.id, apiKey.key, apiKey.status.name))
    }
}
