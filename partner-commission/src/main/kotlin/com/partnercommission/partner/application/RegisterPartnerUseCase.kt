package com.partnercommission.partner.application

import com.partnercommission.partner.domain.Partner
import com.partnercommission.partner.domain.PartnerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RegisterPartnerUseCase(private val partnerRepository: PartnerRepository) {
    fun execute(name: String, email: String): Partner {
        val partner = Partner.create(name, email)
        return partnerRepository.save(partner)
    }
}
