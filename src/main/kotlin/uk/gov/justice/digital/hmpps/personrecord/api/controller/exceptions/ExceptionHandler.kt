package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class ExceptionHandler {

  @ExceptionHandler(PersonRecordNotFoundException::class)
  fun handlePersonRecordNotFoundException(e: PersonRecordNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Not found: ${e.message}",
      ),
    )

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
      ),
    )

  @ExceptionHandler(CanonicalRecordNotFoundException::class)
  fun handleKeyNotFoundException(e: CanonicalRecordNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Not found: ${e.message}",
      ),
    )

  @ExceptionHandler(AuthenticationException::class)
  fun handleUnauthorizedException(e: AuthenticationException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = UNAUTHORIZED,
        userMessage = "Unauthorized: ${e.message}",
      ),
    )
}
