package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonImmigrationStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonImmigrationStatusEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonImmigrationStatusRepository
import java.util.UUID

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconImmigrationStatusController(
  private val prisonImmigrationStatusRepository: PrisonImmigrationStatusRepository,
) {

  @Operation(description = "Store a prisoner sexual orientation")
  @PostMapping("/syscon-sync/immigration-status")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Sexual Orientation created in CPR",
    ),
  )
  @Transactional
  fun createImmigrationStatus(
    @RequestBody immigrationStatus: PrisonImmigrationStatus,
  ): ResponseEntity<PrisonImmigrationStatusResponse> {
    val prisonImmigrationStatusEntity = prisonImmigrationStatusRepository.save(PrisonImmigrationStatusEntity.from(immigrationStatus))
    return ResponseEntity(PrisonImmigrationStatusResponse.from(prisonImmigrationStatusEntity), HttpStatus.CREATED)
  }

  @Operation(description = "Update a prisoner immigration status")
  @PutMapping("/syscon-sync/immigration-status/{cprImmigrationStatusId}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Immigration status updated in CPR",
    ),
  )
  @Transactional
  fun updateImmigrationStatus(
    @PathVariable(name = "cprImmigrationStatusId")
    @Parameter(description = "The identifier of the prison immigration status", required = true)
    cprImmigrationStatusId: UUID,
    @RequestBody immigrationStatus: PrisonImmigrationStatus,
  ): String {
    prisonImmigrationStatusRepository.findByCprImmigrationStatusId(cprImmigrationStatusId)
      ?.update(immigrationStatus)
      ?: throw ResourceNotFoundException(cprImmigrationStatusId.toString())
    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}
