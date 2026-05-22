package com.partnercommission.integration.framework

import com.partnercommission.shared.domain.value.TenantId
import org.assertj.core.api.Assertions.assertThat
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AttributionDbVerifier(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun assertConversionEventExists(tenantId: TenantId, externalId: String) {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM conversion_events WHERE tenant_id = ? AND external_id = ?",
            Int::class.java,
            tenantId.value,
            externalId,
        )
        assertThat(count).`as`("ConversionEvent($externalId)가 저장되어야 합니다").isEqualTo(1)
    }

    fun assertDecisionStatus(decisionId: UUID, expectedStatus: String) {
        val status = jdbcTemplate.queryForObject(
            "SELECT status FROM attribution_decisions WHERE id = ?",
            String::class.java,
            decisionId,
        )
        assertThat(status).`as`("Decision($decisionId) 상태").isEqualTo(expectedStatus)
    }

    fun assertDecisionCount(tenantId: TenantId, expectedCount: Int) {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM attribution_decisions WHERE tenant_id = ?",
            Int::class.java,
            tenantId.value,
        )
        assertThat(count).`as`("테넌트(${tenantId.value})의 Decision 수").isEqualTo(expectedCount)
    }

    fun assertDecisionPartnerId(decisionId: UUID, expectedPartnerId: UUID) {
        val partnerId = jdbcTemplate.queryForObject(
            "SELECT partner_id FROM attribution_decisions WHERE id = ?",
            UUID::class.java,
            decisionId,
        )
        assertThat(partnerId).`as`("Decision($decisionId) partnerId").isEqualTo(expectedPartnerId)
    }

    fun findDecision(decisionId: UUID): Map<String, Any?> {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM attribution_decisions WHERE id = ?",
            decisionId,
        )
    }

    fun findConversionEvent(tenantId: TenantId, externalId: String): Map<String, Any?> {
        return jdbcTemplate.queryForMap(
            "SELECT * FROM conversion_events WHERE tenant_id = ? AND external_id = ?",
            tenantId.value,
            externalId,
        )
    }
}
