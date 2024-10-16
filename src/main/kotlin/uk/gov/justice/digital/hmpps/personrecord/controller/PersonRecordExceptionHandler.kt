package uk.gov.justice.digital.hmpps.personrecord.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PersonRecordExceptionHandler {

  data class ErrorResponse(
    val status: HttpStatus,
    val errorCode: Int? = null,
    val userMessage: String? = null,
    val developerMessage: String? = null,
  )

  @ExceptionHandler(PersonRecordNotFoundException::class)
  fun handlePersonRecordNotFoundException(e: PersonRecordNotFoundException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }
}