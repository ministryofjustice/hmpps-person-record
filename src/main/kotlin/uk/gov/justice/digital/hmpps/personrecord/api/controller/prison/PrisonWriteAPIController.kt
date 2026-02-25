package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_SYSCON_SYNC_WRITE
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonReligionResponseBody
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic.PrisonReligion
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Tag(name = "HMPPS Person API")
@RestController
@PreAuthorize("hasRole('$PERSON_RECORD_SYSCON_SYNC_WRITE')") // TODO: new role?!?
class PrisonWriteAPIController(
  private val prisonReligionRepository: PrisonReligionRepository,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Operation(
    description = """Save prison religion record by Prison Number. Role required is **$PERSON_RECORD_SYSCON_SYNC_WRITE**.""", // TODO: new role?!?
    security = [SecurityRequirement(name = "api-role")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "201",
      description = "Created",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = PrisonReligionResponseBody::class),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(hidden = true),
        ),
      ],
    ),
    ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(hidden = true),
        ),
      ],
    ),
  )
  @PostMapping("/person/prison/{prisonerNumber}/religion")
  @Transactional
  fun save(
    @PathVariable("prisonerNumber") prisonerNumber: String,
    @RequestBody prisonReligionRequest: PrisonReligion,
  ): ResponseEntity<PrisonReligionResponseBody> {
    PrisonReligionEntity.from(prisonerNumber, prisonReligionRequest)
    val personEntity = personRepository.findByPrisonNumber(prisonerNumber) ?: throw ResourceNotFoundException("Person with $prisonerNumber not found")
    val existingCurrentPrisonReligionEntity = prisonReligionRepository.findByPrisonNumber(prisonerNumber).firstOrNull { it.prisonRecordType == PrisonRecordType.CURRENT }
    if (existingCurrentPrisonReligionEntity != null && prisonReligionRequest.current) {
      throw IllegalArgumentException("Person $prisonerNumber already has a current religion")
    }

    val prisonReligionEntity = prisonReligionRepository.save(PrisonReligionEntity.from(prisonerNumber, prisonReligionRequest))
      .also {
        if (prisonReligionRequest.current) {
          personEntity.religion = prisonReligionRequest.religionCode
          personService.processPerson(Person.from(personEntity)) { personEntity }
        }
      }

    val responseBody = PrisonReligionResponseBody.from(prisonerNumber, prisonReligionRequest.nomisReligionId, prisonReligionEntity.updateId)
    return ResponseEntity(responseBody, HttpStatus.CREATED)
  }

  @PutMapping("/person/prison/{prisonerNumber}/religion/{cprReligionId}")
  fun update(
    @PathVariable("prisonerNumber") prisonerNumber: String,
    @PathVariable("cprReligionId") cprReligionId: String,
    @RequestBody prisonReligionRequest: PrisonReligion,
  ) {
  }
}
