package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.QUEUE_ADMIN}')") // TODO change me :-)
class SysconSyncController {

  @Operation(description = "Create a prison record")
  @PutMapping("/syscon-sync/{prisonNumber}")
  @ApiResponses(
    ApiResponse(responseCode = "201", description = "CREATED"),
    ApiResponse(
      responseCode = "404",
      description = "Requested resource not found.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  fun create(
    @NotBlank
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody prisoner: Prisoner,
  ): ResponseEntity<String> {
    log.info("Prisoner {} in body {}", prisonNumber, prisoner.prisonNumber)
    return ResponseEntity.status(CREATED).body("Record Created")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
