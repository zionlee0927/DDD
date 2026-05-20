package com.partnercommission.tracking.application.service

import com.partnercommission.shared.domain.value.PartnerId
import com.partnercommission.shared.domain.value.TenantId
import com.partnercommission.tracking.application.port.`in`.CreateTrackingLinkUseCase
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.repository.TrackingLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateTrackingLinkService(private val trackingLinkRepository: TrackingLinkRepository) : CreateTrackingLinkUseCase {
    override fun execute(tenantId: TenantId, partnerId: PartnerId, targetUrl: String): TrackingLink {
        val link = TrackingLink.create(tenantId, partnerId, targetUrl)
        return trackingLinkRepository.save(link)
    }
}
