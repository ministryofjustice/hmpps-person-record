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
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.PersonRecordNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.PersonIdentifierRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Tag(name = "Search")
@RestController
@PreAuthorize("hasRole('ROLE_CORE_PERSON_RECORD_API__SEARCH__RO')")
class SearchController(
  private val personRepository: PersonRepository,
) {

  @Operation(description = "Search for person record and associated records with a CRN within the system")
  @GetMapping("/search/offender/{crn}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "400",
      description = "Invalid request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
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
  ): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByCrn(crn), crn)
  }

  @Operation(description = "Search for person record and associated records with a prisoner number within the system")
  @GetMapping("/search/prisoner/{prisonNumber}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "400",
      description = "Invalid request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
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
  ): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByPrisonNumberAndSourceSystem(prisonNumber), prisonNumber)
  }

  @Operation(description = "Search for person record and associated records with a defendant identifier within the system")
  @GetMapping("/search/defendant/{defendantId}")
  @ApiResponses(
    ApiResponse(responseCode = "200", description = "OK"),
    ApiResponse(
      responseCode = "400",
      description = "Invalid request.",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
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
    @Parameter(description = "The identifier of the probation source system (COMMON_PLATFORM)", required = true)
    defendantId: String,
  ): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByDefendantId(defendantId), defendantId)
  }

  private fun handlePersonRecord(personEntity: PersonEntity?, identifier: String): List<PersonIdentifierRecord> = when {
    personEntity != PersonEntity.empty -> buildListOfLinkedRecords(personEntity!!)
    else -> throw PersonRecordNotFoundException(identifier)
  }

  private fun buildListOfLinkedRecords(personEntity: PersonEntity): List<PersonIdentifierRecord> {
    return personEntity.personKey?.personEntities?.mapNotNull {
      buildIdentifierRecord(it)
    } ?: listOfNotNull(buildIdentifierRecord(personEntity))
  }

  private fun buildIdentifierRecord(personEntity: PersonEntity): PersonIdentifierRecord? {
    return when (personEntity.sourceSystem) {
      SourceSystemType.DELIUS -> PersonIdentifierRecord(id = personEntity.crn!!, SourceSystemType.DELIUS.name)
      SourceSystemType.NOMIS -> PersonIdentifierRecord(id = personEntity.prisonNumber!!, SourceSystemType.NOMIS.name)
      SourceSystemType.COMMON_PLATFORM -> PersonIdentifierRecord(id = personEntity.defendantId!!, SourceSystemType.COMMON_PLATFORM.name)
      else -> null
    }
  }
}
