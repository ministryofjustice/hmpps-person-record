package uk.gov.justice.digital.hmpps.personrecord.controller.advice

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PersonRecordControllerAdvice {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleMessageNotReadableException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Incorrect parameters exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Incorrect parameters exception: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("EntityNotFoundException exception: {}", e.message, e)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Entity Not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleDataIntegrityViolationException(e: Exception): ResponseEntity<ErrorResponse> {
    val errorMessage = StringUtils.substringBetween(e.cause?.message, "Detail:", ".]")
    log.info("DataIntegrityViolationException exception: ${e.message}", e)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          userMessage = "A person record with these details already exists:$errorMessage",
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
