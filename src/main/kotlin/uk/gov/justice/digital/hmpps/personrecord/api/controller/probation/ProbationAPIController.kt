package uk.gov.justice.digital.hmpps.personrecord.api.controller.probation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PROBATION_API_READ_WRITE
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtProbationLinkEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.CourtProbationLinkRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Tag(name = "HMPPS CPR Probation API")
@RestController
@PreAuthorize("hasRole('$PROBATION_API_READ_WRITE')")
class ProbationAPIController(
  private val personRepository: PersonRepository,
  private val createUpdateService: CreateUpdateService,
  private val courtProbationLinkRepository: CourtProbationLinkRepository,
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
  fun createProbationRecord(
    @PathVariable(name = "defendantId") defendantId: String,
    @RequestBody probationCase: ProbationCase,
  ) {
    probationCase.createPerson()
    probationCase.storeLink(defendantId)
  }

  private fun ProbationCase.createPerson() {
    createUpdateService.processPerson(Person.from(this)) {
      this.identifiers.crn?.let {
        personRepository.findByCrn(it)
      }
    }
  }

  private fun ProbationCase.storeLink(defendantId: String) = this.identifiers.crn?.let {
    courtProbationLinkRepository.save(CourtProbationLinkEntity.from(defendantId, it))
  }
}
