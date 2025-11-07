package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientationResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonSexualOrientationEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonSexualOrientationRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconSexualOrientationController(
  private val prisonSexualOrientationRepository: PrisonSexualOrientationRepository,
) {

  @Operation(description = "Store a prisoner sexual orientation")
  @PutMapping("/syscon-sync/sexual-orientation")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Sexual Orientation created in CPR",
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @Transactional
  fun insertSexualOrientation(
    @RequestBody sexualOrientation: PrisonSexualOrientation,
  ): ResponseEntity<PrisonSexualOrientationResponse> {
    val prisonSexualOrientationEntity = prisonSexualOrientationRepository.save(PrisonSexualOrientationEntity.from(sexualOrientation))
    return ResponseEntity(PrisonSexualOrientationResponse.from(prisonSexualOrientationEntity), HttpStatus.CREATED)
  }
}
