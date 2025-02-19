package uk.gov.justice.digital.hmpps.personrecord.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.PersonKeyNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.PersonRecordNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@Tag(name = "Search")
@RestController
@PreAuthorize("hasRole('${Roles.SEARCH_API_READ_ONLY}')")
class SearchController(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,

) {

  @Operation(description = "Retrieve person record by UUID")
  @GetMapping("/search/person/{uuid}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Requested resource not found.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  fun getCanonicalRecord(
    @PathVariable(name = "uuid") uuid: UUID,
  ): CanonicalRecord = createCanonicalRecord(personKeyRepository.findByPersonId(uuid), uuid)

  @Operation(description = "Search for person record and associated records with a CRN within the system")
  @GetMapping("/search/offender/{crn}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Requested resource not found.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  fun searchByCrn(
    @NotBlank
    @PathVariable(name = "crn")
    @Parameter(description = "The identifier of the probation source system (nDelius)", required = true)
    crn: String,
  ): List<PersonIdentifierRecord> = handlePersonRecord(personRepository.findByCrn(crn), crn)

  @Operation(description = "Search for person record and associated records with a prison number within the system")
  @GetMapping("/search/prisoner/{prisonNumber}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Requested resource not found.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  fun searchByPrisonNumber(
    @NotBlank
    @PathVariable(name = "prisonNumber")
    @Parameter(description = "The identifier of the offender source system (NOMIS)", required = true)
    prisonNumber: String,
  ): List<PersonIdentifierRecord> = handlePersonRecord(personRepository.findByPrisonNumber(prisonNumber), prisonNumber)

  @Operation(description = "Search for person record and associated records with a defendant identifier within the system")
  @GetMapping("/search/defendant/{defendantId}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "404",
      description = "Requested resource not found.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
    ApiResponse(
      responseCode = "500",
      description = "Unrecoverable error occurred whilst processing request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  fun searchByDefendantId(
    @NotBlank
    @PathVariable(name = "defendantId")
    @Parameter(description = "The identifier of the HMCTS (courts and tribunals) source system", required = true)
    defendantId: String,
  ): List<PersonIdentifierRecord> = handlePersonRecord(personRepository.findByDefendantId(defendantId), defendantId)

  private fun handlePersonRecord(personEntity: PersonEntity?, identifier: String): List<PersonIdentifierRecord> = when {
    personEntity != PersonEntity.empty -> buildListOfLinkedRecords(personEntity)
    else -> throw PersonRecordNotFoundException(identifier)
  }

  private fun createCanonicalRecord(personKeyEntity: PersonKeyEntity?, uuid: UUID): CanonicalRecord = when {
    personKeyEntity != PersonEntity.empty -> CanonicalRecord.from(personKeyEntity)
    else -> throw PersonKeyNotFoundException(uuid)
  }

  private fun buildListOfLinkedRecords(personEntity: PersonEntity): List<PersonIdentifierRecord> = personEntity.personKey?.personEntities?.mapNotNull {
    buildIdentifierRecord(it)
  } ?: listOfNotNull(buildIdentifierRecord(personEntity))

  private fun buildIdentifierRecord(personEntity: PersonEntity): PersonIdentifierRecord? = personEntity.extractSourceSystemId()?.let { PersonIdentifierRecord(id = it, personEntity.sourceSystem.name) }
}
