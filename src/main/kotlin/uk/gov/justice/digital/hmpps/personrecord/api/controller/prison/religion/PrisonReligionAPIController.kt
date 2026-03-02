package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.religion

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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.handler.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.handler.PrisonReligionUpdateHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionUpdateRequestBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class PrisonReligionAPIController(
  private val prisonReligionInsertHandler: PrisonReligionInsertHandler,
  private val prisonReligionUpdateHandler: PrisonReligionUpdateHandler,
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
          schema = Schema(implementation = PrisonReligionResponseBody::class),
        ),
      ],
    ),
  )
  @PostMapping("/person/prison/{prisonNumber}/religion")
  fun save(
    @PathVariable("prisonNumber") prisonNumber: String,
    @RequestBody prisonReligionRequest: PrisonReligion,
  ): ResponseEntity<PrisonReligionResponseBody> {
    val prisonReligionMapping = prisonReligionInsertHandler.handleInsert(prisonNumber, prisonReligionRequest)
    val responseBody = PrisonReligionResponseBody(prisonNumber, prisonReligionMapping)
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
          schema = Schema(implementation = PrisonReligionResponseBody::class),
        ),
      ],
    ),
  )
  @PutMapping("/person/prison/{prisonNumber}/religion/{cprReligionId}")
  fun update(
    @PathVariable("prisonNumber") prisonNumber: String,
    @PathVariable("cprReligionId") cprReligionId: String,
    @RequestBody requestBody: PrisonReligionUpdateRequestBody,
  ): ResponseEntity<PrisonReligionResponseBody> {
    val prisonReligionMapping = prisonReligionUpdateHandler.handleUpdate(cprReligionId, requestBody)
    val responseBody = PrisonReligionResponseBody(prisonNumber, prisonReligionMapping)
    return ResponseEntity(responseBody, HttpStatus.OK)
  }
}
