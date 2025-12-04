package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonSexualOrientationEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconSexualOrientationController(
  private val prisonSexualOrientationRepository: PrisonSexualOrientationRepository,
) {

  @Operation(description = "Creates a new prison sexual orientation record, or updates the existing one if a record matching the prison number is found")
  @PostMapping("/syscon-sync/sexual-orientation/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Sexual Orientation saved in CPR",
    ),
  )
  @Transactional
  fun saveSexualOrientation(
    @PathVariable("prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody sexualOrientation: PrisonSexualOrientation,
  ): String {
    prisonSexualOrientationRepository.findByPrisonNumber(prisonNumber)
      ?.update(sexualOrientation)
      ?: prisonSexualOrientationRepository.save(PrisonSexualOrientationEntity.from(prisonNumber, sexualOrientation))

    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}
