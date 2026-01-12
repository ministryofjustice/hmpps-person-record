package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${Roles.PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconReligionController(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository
) {

  @Operation(description = "Updates the prison religion records for the given prison number")
  @PostMapping("/syscon-sync/religion/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Religion saved in CPR",
    ),
  )
  @Transactional
  fun saveReligions(
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
    @Valid @RequestBody religionRequest: PrisonReligionRequest,
  ): String {
    prisonReligionRepository.deleteByPrisonNumber(prisonNumber)

    prisonReligionRepository.saveAll(
      religionRequest.religions.map { PrisonReligionEntity.from(prisonNumber, it) },
    )

    var count = 0
    religionRequest.religions.forEach {
      if (it.current) count++
    }
    if (count > 1) throw IllegalArgumentException("More than one current religion was sent for $religionRequest")
    if (count == 0) throw IllegalArgumentException("No current religion was sent for $religionRequest")
    val currentReligion = religionRequest.religions.firstOrNull { it.current } ?: throw IllegalArgumentException("No Current religion was found for $religionRequest")


    val person = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)
    person.religion = currentReligion.religionCode
    personService.processPerson(Person.from(person)) { person }

    return OK
  }

  companion object {
    private const val OK = "OK"
  }
}
