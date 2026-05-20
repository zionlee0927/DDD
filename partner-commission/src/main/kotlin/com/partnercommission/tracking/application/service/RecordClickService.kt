package com.partnercommission.tracking.application.service

import com.partnercommission.tracking.application.port.`in`.RecordClickUseCase
import com.partnercommission.tracking.domain.aggregate.Click
import com.partnercommission.tracking.domain.aggregate.TrackingLink
import com.partnercommission.tracking.domain.repository.ClickRepository
import com.partnercommission.tracking.domain.repository.TrackingLinkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RecordClickService(
    private val trackingLinkRepository: TrackingLinkRepository,
    private val clickRepository: ClickRepository
) : RecordClickUseCase {
    override fun execute(trackingCode: String, ipAddress: String, userAgent: String): TrackingLink {
        val link = trackingLinkRepository.findByTrackingCode(trackingCode)
            ?: throw IllegalArgumentException("Tracking link not found")
        val click = Click.create(link.tenantId, trackingCode, ipAddress, userAgent)
        clickRepository.save(click)
        return link
    }
}
