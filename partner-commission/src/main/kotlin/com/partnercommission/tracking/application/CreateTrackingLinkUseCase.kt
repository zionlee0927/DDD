package com.partnercommission.tracking.application

import com.partnercommission.shared.domain.PartnerId
import com.partnercommission.shared.domain.TenantId
import com.partnercommission.tracking.domain.TrackingLink
import com.partnercommission.tracking.domain.TrackingLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CreateTrackingLinkUseCase(private val trackingLinkRepository: TrackingLinkRepository) {
    fun execute(tenantId: TenantId, partnerId: PartnerId, targetUrl: String): TrackingLink {
        val link = TrackingLink.create(tenantId, partnerId, targetUrl)
        return trackingLinkRepository.save(link)
    }
}
