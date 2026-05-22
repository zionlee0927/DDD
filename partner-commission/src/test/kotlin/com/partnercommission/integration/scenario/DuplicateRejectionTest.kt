package com.partnercommission.integration.scenario

import com.partnercommission.attribution.domain.exception.DuplicateConversionException
import com.partnercommission.integration.framework.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("중복 전환 거부")
class DuplicateRejectionTest : BaseIntegrationTest() {

    @Test
    @DisplayName("동일 externalId 재전송 → DuplicateConversionException")
    fun duplicateConversion() {
        val ctx = baseContext(externalId = "order-dup")
        flowExecutor.executeFullFlow(ctx)

        assertThatThrownBy {
            flowExecutor.executeFullFlow(ctx.copy(externalId = "order-dup"))
        }.isInstanceOf(DuplicateConversionException::class.java)
    }
}
