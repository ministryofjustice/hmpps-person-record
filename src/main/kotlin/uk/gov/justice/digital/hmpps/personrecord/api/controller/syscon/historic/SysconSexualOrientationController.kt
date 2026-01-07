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
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconSexualOrientationController(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Operation(description = "Update the sexual orientation by prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @PostMapping("/syscon-sync/sexual-orientation/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Sexual Orientation updated in CPR",
    ),
  )
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun updateSexualOrientation(
    @PathVariable @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true) prisonNumber: String,
    @RequestBody sexualOrientation: PrisonSexualOrientation,
  ): String {
    personRepository.findByPrisonNumber(prisonNumber)?.processUpdate(sexualOrientation)
      ?: throw ResourceNotFoundException(prisonNumber)
    return OK
  }

  private fun PersonEntity.processUpdate(prisonSexualOrientation: PrisonSexualOrientation) {
    val person = Person.from(
      this.apply {
        sexualOrientation = prisonSexualOrientation.sexualOrientationCode?.let { SexualOrientation.fromPrison(it) }
      },
    )
    personService.processPerson(person) { this }
  }

  companion object {
    private const val OK = "OK"
  }
}
