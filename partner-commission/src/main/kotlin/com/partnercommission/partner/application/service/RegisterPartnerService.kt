package com.partnercommission.partner.application.service

import com.partnercommission.partner.application.port.`in`.RegisterPartnerUseCase
import com.partnercommission.partner.domain.aggregate.Partner
import com.partnercommission.partner.domain.repository.PartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterPartnerService(private val partnerRepository: PartnerRepository) : RegisterPartnerUseCase {
    override fun execute(name: String, email: String): Partner {
        val partner = Partner.create(name, email)
        return partnerRepository.save(partner)
    }
}
