package uk.gov.justice.digital.hmpps.personrecord.api.controller.syscon.historic

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligionRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res.SysconReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
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

  @Operation(description = "Save the prison religion records for the given prison number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.")
  @PostMapping("/syscon-sync/religion/{prisonNumber}")
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Religions saved in CPR",
    ),
  )
  @Transactional
  fun saveReligions(
    @PathVariable @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true) prisonNumber: String,
    @Valid @RequestBody religionRequest: PrisonReligionRequest,
  ): ResponseEntity<SysconReligionResponseBody> {
    val currentPrisonReligion = religionRequest.extractExactlyOneCurrentReligion()
    val person = validateRequest(prisonNumber, religionRequest)

    val cprReligionIdByNomisId = saveReligionsMapped(prisonNumber, religionRequest)

    person.religion = currentPrisonReligion.religionCode
    personService.processPerson(Person.from(person)) { person }

    val religionResponseBody = SysconReligionResponseBody.from(prisonNumber, cprReligionIdByNomisId)
    return ResponseEntity.status(HttpStatus.CREATED).body(religionResponseBody)
  }

  private fun validateRequest(prisonerNumber: String, religionRequest: PrisonReligionRequest): PersonEntity {
    val person = personRepository.findByPrisonNumber(prisonerNumber) ?: throw ResourceNotFoundException(prisonerNumber)
    if (prisonReligionRepository.findByPrisonNumber(prisonerNumber).isNotEmpty()) throw ConflictException("Religion(s) already exists for $prisonerNumber")

    val map = HashMap<String, String>()
    religionRequest.religions.forEach {
      if (map.contains(it.nomisReligionId)) throw IllegalArgumentException("Duplicate nomis religion id were detected for $prisonerNumber")
      else map[it.nomisReligionId] = it.nomisReligionId
    }
    return person
  }

  private fun saveReligionsMapped(
    prisonerNumber: String,
    prisonReligionRequest: PrisonReligionRequest,
  ): Map<String, String> {
    val cprReligionIdByNomisId = HashMap<String, String>()
    prisonReligionRequest.religions.forEach { prisonReligion ->
      val religionEntity = PrisonReligionEntity.from(prisonerNumber, prisonReligion)
      val prisonReligionEntity = prisonReligionRepository.save(religionEntity)
      cprReligionIdByNomisId[prisonReligion.nomisReligionId] = prisonReligionEntity.updateId.toString()
    }
    return cprReligionIdByNomisId
  }

  private fun PrisonReligionRequest.extractExactlyOneCurrentReligion(): PrisonReligion {
    val currentReligionCount = this.religions.filter { it.current }
    return when {
      currentReligionCount.size != 1 -> throw IllegalArgumentException("Exactly one current prison religion must be sent for $this")
      else -> currentReligionCount.first()
    }
  }

  companion object {
    private const val OK = "OK"
  }
}
