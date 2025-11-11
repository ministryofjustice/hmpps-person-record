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
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatusResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonDisabilityStatusEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonDisabilityStatusRepository
import java.util.UUID

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconDisabilityStatusController(
  private val prisonDisabilityStatusRepository: PrisonDisabilityStatusRepository,
) {

  @Operation(description = "Store a prisoner disability status")
  @PostMapping("/syscon-sync/disability-status")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Disability Status created in CPR",
    ),
  )
  @Transactional
  fun insertDisabilityStatus(
    @RequestBody disabilityStatus: PrisonDisabilityStatus,
  ): ResponseEntity<PrisonDisabilityStatusResponse> {
    val prisonDisabilityStatusEntity = prisonDisabilityStatusRepository.save(PrisonDisabilityStatusEntity.from(disabilityStatus))
    return ResponseEntity(PrisonDisabilityStatusResponse.from(prisonDisabilityStatusEntity), HttpStatus.CREATED)
  }

  @Operation(description = "Update a prisoner disability status")
  @PutMapping("/syscon-sync/disability-status/{cprDisabilityStatusId}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Disability Status updated in CPR",
    ),
  )
  @Transactional
  fun updateDisabilityStatus(
    @PathVariable(name = "cprDisabilityStatusId")
    @Parameter(description = "The identifier of the prison disability status", required = true)
    cprDisabilityStatusId: UUID,
    @RequestBody disabilityStatus: PrisonDisabilityStatus,
  ): String {
    prisonDisabilityStatusRepository.findByCprDisabilityStatusId(cprDisabilityStatusId)
      ?.update(disabilityStatus)
      ?: throw ResourceNotFoundException(cprDisabilityStatusId.toString())
    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}
