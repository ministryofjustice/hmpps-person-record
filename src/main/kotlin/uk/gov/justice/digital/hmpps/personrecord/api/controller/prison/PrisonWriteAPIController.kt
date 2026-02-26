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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PRISON_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.handler.PrisonReligionInsertHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$PRISON_API_READ_WRITE')")
class PrisonWriteAPIController(private val prisonReligionInsertHandler: PrisonReligionInsertHandler) {

  @Operation(
    description = """Save prison religion record by Prison Number. Role required is **$PRISON_API_READ_WRITE**.""",
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
    ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(hidden = true),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(hidden = true),
        ),
      ],
    ),
  )
  @PostMapping("/person/prison/{prisonerNumber}/religion")
  fun save(
    @PathVariable("prisonerNumber") prisonNumber: String,
    @RequestBody prisonReligionRequest: PrisonReligion,
  ): ResponseEntity<PrisonReligionResponseBody> {
    val prisonReligionMapping = prisonReligionInsertHandler.handleInsert(prisonNumber, prisonReligionRequest)
    val responseBody = PrisonReligionResponseBody.from(prisonNumber, prisonReligionMapping)
    return ResponseEntity(responseBody, HttpStatus.CREATED)
  }
}
