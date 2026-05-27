package com.partnercommission.commission.domain.service

import com.partnercommission.commission.domain.value.CommissionStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RevocationPolicyTest {

    private val policy = RevocationPolicy()

    @Test
    fun `PENDING → CANCEL`() {
        assertThat(policy.decide(CommissionStatus.PENDING)).isEqualTo(RevocationAction.CANCEL)
    }

    @Test
    fun `CONFIRMED → DEDUCT`() {
        assertThat(policy.decide(CommissionStatus.CONFIRMED)).isEqualTo(RevocationAction.DEDUCT)
    }

    @Test
    fun `SETTLED → DEDUCT`() {
        assertThat(policy.decide(CommissionStatus.SETTLED)).isEqualTo(RevocationAction.DEDUCT)
    }

    @Test
    fun `CANCELLED → IGNORE`() {
        assertThat(policy.decide(CommissionStatus.CANCELLED)).isEqualTo(RevocationAction.IGNORE)
    }
}
