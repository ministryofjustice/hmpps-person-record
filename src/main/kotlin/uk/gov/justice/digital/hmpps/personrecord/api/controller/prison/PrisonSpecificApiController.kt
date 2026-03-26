package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.handler.prison.PrisonGetHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
class PrisonSpecificApiController(private val prisonGetHandler: PrisonGetHandler) {

  @Operation(
    description = """Retrieve person record by Prison Number. Role required is **$API_READ_ONLY** . 
      For Identifiers the crn, prisonNumber, defendantId, cids come from all records related to this person.
      The other Identifiers come from just this person
      **cprUUID is not supplied on this endpoint.**
      In addition to the person data being returned, the response also includes a list of the prison religions associated with the person
      and a prison specific representation of alias & identifiers.
      """,
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/person/prison/specific/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
    ApiResponse(
      responseCode = "301",
      description = "Permanent Redirect",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
  )
  fun getByPrisonNumberPrisonSpecific(@PathVariable(name = "prisonNumber") prisonNumber: String): ResponseEntity<PrisonCanonicalRecord> {
    val result = prisonGetHandler.get(prisonNumber)
    return TODO()
  }
}
