package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonDisabilityStatus
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconDisabilityStatusController(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Operation(description = """Update the disability status by prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.""")
  @PostMapping("/syscon-sync/disability-status/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Disability status updated in CPR",
    ),
  )
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun updateDisabilityStatus(
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody prisonDisabilityStatus: PrisonDisabilityStatus,
  ): String {
    personRepository.findByPrisonNumber(prisonNumber)
      ?.processUpdate(prisonDisabilityStatus)
      ?: throw ResourceNotFoundException(prisonNumber)
    return OK
  }

  private fun PersonEntity.processUpdate(prisonDisabilityStatus: PrisonDisabilityStatus) {
    val person = Person.from(
      this.apply {
        disability = prisonDisabilityStatus.disability
      },
    )
    personService.processPerson(person) { this }
  }

  companion object {
    private const val OK = "OK"
  }
}
