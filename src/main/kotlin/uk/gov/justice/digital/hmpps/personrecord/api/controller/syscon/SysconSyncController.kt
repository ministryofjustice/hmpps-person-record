package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.SysconPersonUpdateHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconUpdatePersonResponse

@Tag(name = "Syscon Sync")
@RestController
@Profile("!preprod & !prod")
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconSyncController(
  private val sysconPersonUpdateHandler: SysconPersonUpdateHandler,
) {

  @Operation(description = "Update a prison record by prison number")
  @PutMapping("/syscon-sync/person/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Data updated in CPR",
    ),
  )
  fun update(
    @NotBlank
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody prisoner: Prisoner,
  ): ResponseEntity<SysconUpdatePersonResponse> {
    val responseBody = sysconPersonUpdateHandler.handle(prisonNumber, prisoner)
    return ResponseEntity.status(HttpStatus.OK).body(responseBody)
  }
}
