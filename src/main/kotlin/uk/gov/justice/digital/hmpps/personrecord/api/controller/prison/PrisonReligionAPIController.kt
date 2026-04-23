package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
@RequestMapping("/person/prison")
class PrisonReligionAPIController(
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
  private val prisonReligionUpdateHandler: PrisonReligionUpdateHandler,
  private val prisonReligionRepository: PrisonReligionRepository,
) {

  @Operation(
    description = """Save prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Created",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonReligionSaveResponse::class),
        ),
      ],
    ),
  )
  @PostMapping("/{prisonNumber}/religion")
  fun savePrisonReligion(
    @PathVariable("prisonNumber") prisonNumber: String,
    @RequestBody prisonReligionHistoryRequest: PrisonReligionHistory,
  ): ResponseEntity<PrisonReligionSaveResponse> {
    val prisonReligionMapping = prisonReligionInsertHandler.handleInsert(prisonNumber, prisonReligionHistoryRequest)
    val responseBody = PrisonReligionSaveResponse(prisonNumber, prisonReligionMapping)
    return ResponseEntity(responseBody, HttpStatus.CREATED)
  }

  @Operation(
    description = """Update prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonReligionSaveResponse::class),
        ),
      ],
    ),
  )
  @PutMapping("/{prisonNumber}/religion/{cprReligionId}")
  fun updatePrisonReligion(
    @PathVariable("prisonNumber") prisonNumber: String,
    @PathVariable("cprReligionId") cprReligionId: String,
    @RequestBody requestBody: PrisonReligionUpdateRequest,
  ): ResponseEntity<PrisonReligionSaveResponse> {
    val prisonReligionMapping = prisonReligionUpdateHandler.handleUpdate(cprReligionId, requestBody)
    val responseBody = PrisonReligionSaveResponse(prisonNumber, prisonReligionMapping)
    return ResponseEntity(responseBody, HttpStatus.OK)
  }

  @Operation(
    description = """Get prison religion record by Prison Number. Role required is **${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}**.""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonReligionReadResponse::class),
        ),
      ],
    ),
  )
  @GetMapping("/{prisonNumber}/religion/{cprReligionId}")
  fun getPrisonReligion(
    @PathVariable("prisonNumber") prisonNumber: String,
    @PathVariable("cprReligionId") cprReligionId: String,
  ): ResponseEntity<PrisonReligionReadResponse> {
    val prisonReligionEntity = prisonReligionRepository.findByUpdateId(UUID.fromString(cprReligionId))
      ?: throw ResourceNotFoundException("Prison religion with $cprReligionId not found")
    return ResponseEntity(PrisonReligionReadResponse.from(prisonNumber, prisonReligionEntity), HttpStatus.OK)
  }
}
