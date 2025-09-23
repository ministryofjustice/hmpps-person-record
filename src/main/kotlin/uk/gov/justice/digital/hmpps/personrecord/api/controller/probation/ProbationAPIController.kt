package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService

@Tag(name = "HMPPS CPR Probation API")
@RestController
@PreAuthorize("hasRole('$PROBATION_API_READ_WRITE')")
class ProbationAPIController(
  private val personRepository: PersonRepository,
  private val createUpdateService: CreateUpdateService,
  private val overrideService: OverrideService,
  private val reclusterService: ReclusterService,
) {
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
  @Transactional(isolation = REPEATABLE_READ)
  fun createProbationRecord(
    @PathVariable(name = "defendantId") defendantId: String,
    @RequestBody probationCase: ProbationCase,
  ) {
    val defendant: PersonEntity = retrieveDefendant(defendantId)
    val offender: PersonEntity = createUpdateService.processPerson(Person.from(probationCase)) {
      personRepository.findByCrn(probationCase.identifiers.crn!!)
    }
    overrideService.systemInclude(defendant, offender)
    reclusterService.recluster(offender)
  }

  private fun retrieveDefendant(defendantId: String): PersonEntity = personRepository.findByDefendantId(defendantId) ?: throw ResourceNotFoundException(defendantId)
}
