package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestControllerAdvice
class ExceptionHandler {

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleBadRequest(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Bad request: ${e.message}",
      ),
    )

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Bad request: ${e.message}",
      ),
    )

  @ExceptionHandler(ConflictException::class)
  fun handleBadRequest(e: ConflictException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(CONFLICT)
    .body(
      ErrorResponse(
        status = CONFLICT,
        userMessage = "Conflict: ${e.message}",
      ),
    )

  @ExceptionHandler(RuntimeException::class)
  fun handleInternalServerError(e: RuntimeException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = e.message ?: "Internal Server Error",
      ),
    )

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "Forbidden: ${e.message}",
      ),
    )

  @ExceptionHandler(ResourceNotFoundException::class)
  fun handleKeyNotFoundException(e: ResourceNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "Not found: ${e.message}",
      ),
    )

  @ExceptionHandler(AuthenticationException::class)
  fun handleUnauthorizedException(e: AuthenticationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = UNAUTHORIZED,
        userMessage = "Unauthorized: ${e.message}",
      ),
    )
}
