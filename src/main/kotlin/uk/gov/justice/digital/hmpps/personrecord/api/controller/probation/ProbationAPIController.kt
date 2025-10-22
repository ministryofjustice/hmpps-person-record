package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "HMPPS Person API")
@RestController
class ProbationAPIController(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val reclusterService: ReclusterService,
) {
  @Operation(
    description = """Retrieve person record by CRN. Role required is **$API_READ_ONLY** . 
      For Identifiers the crn, prisonNumber, defendantId, cids come from all records related to this person.
      The other Identifiers come from just this person
      **cprUUID is not supplied on this endpoint.**""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @GetMapping("/person/probation/{crn}")
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
  @PreAuthorize("hasRole('$API_READ_ONLY')")
  fun getRecord(
    @PathVariable(name = "crn") crn: String,
  ): ResponseEntity<*> {
    val personEntity = personRepository.findByCrn(crn)
    return when {
      personEntity == null -> throw ResourceNotFoundException(crn)
      else -> ResponseEntity.ok(CanonicalRecord.from(personEntity))
    }
  }

  @Operation(
    description = """Create person record by CRN. Role required is **$PROBATION_API_READ_WRITE** . 
      Includes all fields relating to core person information""",
    security = [SecurityRequirement(name = "api-role")],
  )
  @PutMapping("/person/probation/{defendantId}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
    ),
  )
  @PreAuthorize("hasRole('$PROBATION_API_READ_WRITE')")
  @Transactional(isolation = REPEATABLE_READ)
  fun createProbationRecord(
    @PathVariable(name = "defendantId") defendantId: String,
    @RequestBody probationCase: ProbationCase,
  ) {
    val masterDefendantId: String? = retrieveDefendant(defendantId).masterDefendantId

    val person = Person.from(probationCase)
    person.masterDefendantId = masterDefendantId

    val offender: PersonEntity = personService.processPerson(person) {
      personRepository.findByCrn(probationCase.identifiers.crn!!)
    }

    reclusterService.recluster(offender)
  }

  private fun retrieveDefendant(defendantId: String): PersonEntity = personRepository.findByDefendantId(defendantId) ?: throw ResourceNotFoundException(defendantId)
}
