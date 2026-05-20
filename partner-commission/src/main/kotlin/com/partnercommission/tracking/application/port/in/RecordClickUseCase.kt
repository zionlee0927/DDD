package com.partnercommission.tracking.application.port.`in`

import com.partnercommission.tracking.domain.aggregate.TrackingLink

interface RecordClickUseCase {
    fun execute(trackingCode: String, ipAddress: String, userAgent: String): TrackingLink
}
