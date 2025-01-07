package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

import org.springframework.http.HttpStatus
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
    .status(HttpStatus.NOT_FOUND)
    .body(
      ErrorResponse(
        status = HttpStatus.NOT_FOUND,
        userMessage = "Not found: ${e.message}",
      ),
    )

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(HttpStatus.FORBIDDEN)
    .body(
      ErrorResponse(
        status = HttpStatus.FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
      ),
    )

  @ExceptionHandler(AuthenticationException::class)
  fun handleUnauthorizedException(e: AuthenticationException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(HttpStatus.UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = HttpStatus.UNAUTHORIZED,
        userMessage = "Unauthorized: ${e.message}",
      ),
    )
}
