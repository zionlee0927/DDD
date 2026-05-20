package com.partnercommission.tracking.application

import com.partnercommission.tracking.domain.Click
import com.partnercommission.tracking.domain.ClickRepository
import com.partnercommission.tracking.domain.TrackingLink
import com.partnercommission.tracking.domain.TrackingLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RecordClickUseCase(
    private val trackingLinkRepository: TrackingLinkRepository,
    private val clickRepository: ClickRepository
) {
    fun execute(trackingCode: String, ipAddress: String, userAgent: String): TrackingLink {
        val link = trackingLinkRepository.findByTrackingCode(trackingCode)
            ?: throw IllegalArgumentException("Tracking link not found")
        val click = Click.create(link.tenantId, trackingCode, ipAddress, userAgent)
        clickRepository.save(click)
        return link
    }
}
