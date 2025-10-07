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
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
class PrisonAPIController(
  private val personRepository: PersonRepository,
) {
  @Operation(
    description = """Retrieve person record by Prison Number. Role required is **$API_READ_ONLY** . 
      For Identifiers the crn, prisonNumber, defendantId, cids come from all records related to this person.
      The other Identifiers come from just this person
      **cprUUID is not supplied on this endpoint.**""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/person/prison/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CanonicalRecord::class),
        ),
      ],
    ),
  )
  fun getRecord(
    @PathVariable(name = "prisonNumber") prisonNumber: String,
  ): ResponseEntity<*> {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
    return when {
      personEntity == null -> throw ResourceNotFoundException(prisonNumber)
      else -> ResponseEntity.ok(CanonicalRecord.from(personEntity))
    }
  }
}
