package com.partnercommission.integration.framework

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tenant.domain.aggregate.Tenant
import com.partnercommission.tenant.domain.repository.TenantRepository
import com.partnercommission.tenant.domain.value.ProgramConfig
import com.partnercommission.tenant.domain.value.TenantStatus
import com.partnercommission.tenant.domain.value.WebhookConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.time.LocalDateTime

@SpringBootTest
@Transactional
@Tag("integration")
abstract class BaseIntegrationTest {

    @Autowired lateinit var flowExecutor: AttributionFlowExecutor
    @Autowired lateinit var tenantRepository: TenantRepository
    @Autowired lateinit var dbVerifier: AttributionDbVerifier
    @Autowired lateinit var entityManager: EntityManager

    protected val tenantId = TenantId()
    protected val partnerId = PartnerId()

    @BeforeEach
    fun setUpBase() {
        tenantRepository.save(
            Tenant.reconstitute(tenantId, "Test Tenant", TenantStatus.ACTIVE, ProgramConfig(), WebhookConfig(), LocalDateTime.now())
        )
    }

    protected fun baseContext(externalId: String = "order-${System.nanoTime()}") =
        AttributionScenarioContext(tenantId = tenantId, partnerId = partnerId, externalId = externalId)

    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
