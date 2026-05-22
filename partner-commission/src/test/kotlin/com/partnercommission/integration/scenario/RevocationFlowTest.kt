package com.partnercommission.integration.scenario

import com.partnercommission.attribution.domain.exception.AttributionNotFoundException
import com.partnercommission.attribution.domain.value.AttributionStatus
import com.partnercommission.integration.framework.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("귀속 철회 흐름")
class RevocationFlowTest : BaseIntegrationTest() {

    @Test
    @DisplayName("ATTRIBUTED → 철회 → REVOKED")
    fun revokeSuccess() {
        val ctx = baseContext()
        flowExecutor.executeFullFlow(ctx)

        val revoked = flowExecutor.executeRevoke(ctx)

        assertThat(revoked.currentStatus()).isEqualTo(AttributionStatus.REVOKED)
    }

    @Test
    @DisplayName("존재하지 않는 전환 철회 → 예외")
    fun revokeNonExistent() {
        val ctx = baseContext(externalId = "non-existent")

        assertThatThrownBy { flowExecutor.executeRevoke(ctx) }
            .isInstanceOf(AttributionNotFoundException::class.java)
    }
}
