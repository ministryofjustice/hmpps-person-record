package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.net.URI

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
    ApiResponse(
      responseCode = "301",
      description = "Permanent Redirect",
      content = [
        Content(schema = Schema(hidden = true)),
      ],
    ),
  )
  fun getRecord(
    @PathVariable(name = "prisonNumber") prisonNumber: String,
  ): ResponseEntity<*> {
    val personEntity = getMergedToPersonIfExist(personRepository.findByPrisonNumber(prisonNumber))
    return when {
      personEntity == null -> throw ResourceNotFoundException(prisonNumber)
      personEntity.prisonNumber != prisonNumber -> ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI("/person/prison/${personEntity.prisonNumber}")).build<Void>()
      else -> ResponseEntity.ok(CanonicalRecord.from(personEntity))
    }
  }

  fun getMergedToPersonIfExist(person: PersonEntity?): PersonEntity? = when {
    person?.mergedTo != null -> getMergedToPersonIfExist(personRepository.findByIdOrNull(id = person.mergedTo!!))
    else -> person
  }
}
