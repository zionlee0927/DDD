package com.partnercommission.shared.infrastructure

import com.partnercommission.attribution.domain.exception.AttributionException
import com.partnercommission.attribution.domain.exception.AttributionNotFoundException
import com.partnercommission.attribution.domain.exception.DuplicateConversionException
import com.partnercommission.attribution.domain.exception.EvidenceRequiredException
import com.partnercommission.attribution.domain.exception.RevocationWindowExpiredException
import com.partnercommission.attribution.domain.exception.TenantNotFoundException
import com.partnercommission.attribution.domain.exception.UnsupportedEvidenceTypeException
import com.partnercommission.commission.domain.exception.CommissionException
import com.partnercommission.commission.domain.exception.CommissionNotFoundException
import com.partnercommission.commission.domain.exception.CommissionNotFoundByAttributionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DuplicateConversionException::class)
    fun handleDuplicate(e: DuplicateConversionException) =
        respond(HttpStatus.CONFLICT, "DUPLICATE_CONVERSION", e)

    @ExceptionHandler(RevocationWindowExpiredException::class)
    fun handleRevocationExpired(e: RevocationWindowExpiredException) =
        respond(HttpStatus.CONFLICT, "REVOCATION_WINDOW_EXPIRED", e)

    @ExceptionHandler(TenantNotFoundException::class, AttributionNotFoundException::class, CommissionNotFoundException::class, CommissionNotFoundByAttributionException::class)
    fun handleNotFound(e: RuntimeException) =
        respond(HttpStatus.NOT_FOUND, "NOT_FOUND", e)

    @ExceptionHandler(EvidenceRequiredException::class)
    fun handleEvidenceRequired(e: EvidenceRequiredException) =
        respond(HttpStatus.BAD_REQUEST, "EVIDENCE_REQUIRED", e)

    @ExceptionHandler(UnsupportedEvidenceTypeException::class)
    fun handleUnsupportedEvidence(e: UnsupportedEvidenceTypeException) =
        respond(HttpStatus.BAD_REQUEST, "UNSUPPORTED_EVIDENCE_TYPE", e)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException) =
        respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e)

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException) =
        respond(HttpStatus.BAD_REQUEST, "INVALID_STATE", e)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("[500] Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(500, "INTERNAL_ERROR", "Internal server error")
        )
    }

    private fun respond(status: HttpStatus, code: String, e: RuntimeException): ResponseEntity<ErrorResponse> {
        if (status.is5xxServerError) {
            log.error("[${status.value()}] $code: ${e.message}", e)
        } else {
            log.warn("[${status.value()}] $code: ${e.message}")
        }
        return ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), code = code, message = e.message ?: "Unknown error")
        )
    }
}
