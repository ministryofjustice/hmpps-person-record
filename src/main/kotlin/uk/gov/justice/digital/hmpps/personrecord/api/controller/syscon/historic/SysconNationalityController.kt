package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonNationality
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconNationalityController(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {
  @Operation(description = "Upserts a nationality record on a matching prison number")
  @PostMapping("/syscon-sync/nationality/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Nationality saved in CPR",
    ),
  )
  @Transactional
  fun saveNationality(
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @RequestBody nationality: PrisonNationality,
  ): String {
    personRepository.findByPrisonNumber(prisonNumber)?.processUpsert(nationality)
      ?: throw ResourceNotFoundException(prisonNumber)

    return OK
  }

  fun PersonEntity.processUpsert(nationality: PrisonNationality) {
    val person = Person.from(this)
    person.nationalities = listOf(NationalityCode.fromPrisonCode(nationality.nationalityCode)).mapNotNull { it }
    personService.processPerson(person) { this }
  }

  companion object {
    private const val OK = "OK"
  }
}
