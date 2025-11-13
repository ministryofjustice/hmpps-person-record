package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconReligionController(
  private val prisonReligionRepository: PrisonReligionRepository
) {

  @Operation(description = "Store a prisoner religion")
  @PostMapping("/syscon-sync/religion")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Religion created in CPR",
    ),
  )
  @Transactional
  fun createReligion(
    @RequestBody religion: PrisonReligion,
  ): ResponseEntity<PrisonReligionResponse> {
    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(religion))
    return ResponseEntity(PrisonReligionResponse.from(prisonReligionEntity), HttpStatus.CREATED)
  }
}