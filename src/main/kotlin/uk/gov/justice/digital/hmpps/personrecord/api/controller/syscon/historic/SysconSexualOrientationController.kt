package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonSexualOrientationEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconSexualOrientationController(
  private val prisonSexualOrientationRepository: PrisonSexualOrientationRepository
) {

  @Operation(description = "Store a prisoner sexual orientation")
  @PutMapping("/syscon-sync/sexual-orientation/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Data created in CPR",
    ),
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
  @Transactional
  fun insertSexualOrientation(
    @NotBlank
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody sexualOrientations: List<PrisonSexualOrientation>,
  ): String {
    prisonSexualOrientationRepository.findAllByPrisonNumber(prisonNumber).clear()
    prisonSexualOrientationRepository.saveAll(PrisonSexualOrientationEntity.fromList(prisonNumber, sexualOrientations))
    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}