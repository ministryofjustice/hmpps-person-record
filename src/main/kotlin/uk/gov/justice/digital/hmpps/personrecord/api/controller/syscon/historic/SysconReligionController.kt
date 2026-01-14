package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "Syscon Sync")
@RestController
@PreAuthorize("hasRole('${PERSON_RECORD_SYSCON_SYNC_WRITE}')")
class SysconReligionController(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) {

  @Operation(description = "Update the prison religion records for the given prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @PostMapping("/syscon-sync/religion/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "Religions saved in CPR",
    ),
  )
  @Transactional(isolation = Isolation.REPEATABLE_READ)
  fun saveReligions(
    @PathVariable @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true) prisonNumber: String,
    @Valid @RequestBody religionRequest: PrisonReligionRequest,
  ): String {
    val currentPrisonReligion = religionRequest.extractExactlyOneCurrentReligion()
    val person = personRepository.findByPrisonNumber(prisonNumber) ?: throw ResourceNotFoundException(prisonNumber)

    prisonReligionRepository.deleteInBulkByPrisonNumber(prisonNumber)
    prisonReligionRepository.saveAll(
      religionRequest.religions.map { PrisonReligionEntity.from(prisonNumber, it) },
    )

    person.religion = currentPrisonReligion.religionCode
    personService.processPerson(Person.from(person)) { person }

    return OK
  }

  private fun PrisonReligionRequest.extractExactlyOneCurrentReligion(): PrisonReligion {
    val currentReligionCount = this.religions.filter { it.current }
    return when {
      currentReligionCount.size > 1 -> throw IllegalArgumentException("More than one current religion was sent for $this")
      currentReligionCount.isEmpty() -> throw IllegalArgumentException("No current religion was sent for $this")
      else -> currentReligionCount.first()
    }
  }

  companion object {
    private const val OK = "OK"
  }
}
