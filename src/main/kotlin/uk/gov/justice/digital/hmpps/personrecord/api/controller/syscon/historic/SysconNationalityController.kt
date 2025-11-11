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
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationalityResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonNationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonNationalityRepository
import java.util.UUID

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconNationalityController(
  private val prisonNationalityRepository: PrisonNationalityRepository,
) {
  @Operation(description = "Store a prisoner nationality")
  @PostMapping("/syscon-sync/nationality")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Nationality created in CPR",
    ),
  )
  @Transactional
  fun createNationality(
    @RequestBody nationality: PrisonNationality,
  ): ResponseEntity<PrisonNationalityResponse> {
    val prisonNationalityEntity = prisonNationalityRepository.save(PrisonNationalityEntity.from(nationality))
    return ResponseEntity(PrisonNationalityResponse.from(prisonNationalityEntity), HttpStatus.CREATED)
  }

  @Operation(description = "Update a prisoner nationality")
  @PutMapping("/syscon-sync/nationality/{cprNationalityId}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Nationality updated in CPR",
    ),
  )
  @Transactional
  fun updateNationality(
    @PathVariable(name = "cprNationalityId")
    @Parameter(description = "The identifier of the prison nationality", required = true)
    cprNationalityId: UUID,
    @RequestBody nationality: PrisonNationality,
  ): String {
    prisonNationalityRepository.findByCprNationalityId(cprNationalityId)
      ?.update(nationality)
      ?: throw ResourceNotFoundException(cprNationalityId.toString())
    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}
