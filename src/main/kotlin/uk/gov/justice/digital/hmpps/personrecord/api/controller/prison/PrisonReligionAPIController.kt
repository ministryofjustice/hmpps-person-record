package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonReligionUpdateHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionReadResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionSaveResponse
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionHistory
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import java.util.UUID

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
@RequestMapping("/person/prison", produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonReligionAPIController(
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
  private val prisonReligionUpdateHandler: PrisonReligionUpdateHandler,
  private val prisonReligionRepository: PrisonReligionRepository,
) {

  @Operation(
    description = """Save prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{prisonNumber}/religion")
  fun savePrisonReligion(
    @PathVariable prisonNumber: String,
    @RequestBody prisonReligionHistoryRequest: PrisonReligionHistory,
  ): PrisonReligionSaveResponse {
    val prisonReligionMapping = prisonReligionInsertHandler.handleInsert(prisonNumber, prisonReligionHistoryRequest)
    return PrisonReligionSaveResponse(prisonNumber, prisonReligionMapping)
  }

  @Operation(
    description = """Update prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/{prisonNumber}/religion/{cprReligionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updatePrisonReligion(
    @PathVariable prisonNumber: String,
    @PathVariable cprReligionId: String,
    @RequestBody requestBody: PrisonReligionUpdateRequest,
  ) {
    prisonReligionUpdateHandler.handleUpdate(cprReligionId, requestBody)
  }

  @Operation(
    description = """Get prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/{prisonNumber}/religion/{cprReligionId}")
  fun getPrisonReligion(
    @PathVariable prisonNumber: String,
    @PathVariable cprReligionId: String,
  ): ResponseEntity<PrisonReligionReadResponse> {
    val prisonReligionEntity = prisonReligionRepository.findByUpdateId(UUID.fromString(cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with $cprReligionId not found")
    return ResponseEntity(PrisonReligionReadResponse.from(prisonNumber, prisonReligionEntity), HttpStatus.OK)
  }
}
