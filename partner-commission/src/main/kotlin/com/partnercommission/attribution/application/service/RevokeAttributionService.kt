package com.partnercommission.attribution.application.service

import com.partnercommission.attribution.application.port.`in`.RevokeAttributionCommand
import com.partnercommission.attribution.application.port.`in`.RevokeAttributionUseCase
import com.partnercommission.attribution.application.port.out.LoadTenantConfigPort
import com.partnercommission.attribution.domain.aggregate.AttributionDecision
import com.partnercommission.attribution.domain.exception.AttributionNotFoundException
import com.partnercommission.attribution.domain.repository.AttributionDecisionRepository
import com.partnercommission.attribution.domain.repository.ConversionEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class RevokeAttributionService(
    private val conversionEventRepository: ConversionEventRepository,
    private val attributionDecisionRepository: AttributionDecisionRepository,
    private val loadTenantConfigPort: LoadTenantConfigPort,
) : RevokeAttributionUseCase {

    override fun execute(command: RevokeAttributionCommand): AttributionDecision {
        val conversionEvent = conversionEventRepository.findByTenantAndExternalId(command.tenantId, command.externalId)
            ?: throw AttributionNotFoundException(command.externalId)

        val decision = attributionDecisionRepository.findByConversionEventId(conversionEvent.id)
            ?: throw AttributionNotFoundException(command.externalId)

        val config = loadTenantConfigPort.loadConfig(command.tenantId)

        decision.revoke(LocalDateTime.now(), config.revocationWindowDays)

        return attributionDecisionRepository.save(decision)
    }
}
