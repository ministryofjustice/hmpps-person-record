package uk.gov.justice.digital.hmpps.personrecord.api.controller

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
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
@PreAuthorize("hasRole('ROLE_CORE_PERSON_RECORD_API__SEARCH__RO')")
class SearchController(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
) {

  @GetMapping("/search/offender/{crn}")
  fun searchByCrn(@NotBlank @PathVariable(name = "crn") crn: String): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByCrn(crn), Pair(EventKeys.CRN, crn))
  }

  @GetMapping("/search/prisoner/{prisonNumber}")
  fun searchByPrisonNumber(@NotBlank @PathVariable(name = "prisonNumber") prisonNumber: String): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByPrisonNumberAndSourceSystem(prisonNumber), Pair(EventKeys.PRISON_NUMBER, prisonNumber))
  }

  @GetMapping("/search/defendant/{defendantId}")
  fun searchByDefendantId(@NotBlank @PathVariable(name = "defendantId") defendantId: String): List<PersonIdentifierRecord> {
    return handlePersonRecord(personRepository.findByDefendantId(defendantId), Pair(EventKeys.DEFENDANT_ID, defendantId))
  }

  private fun handlePersonRecord(personEntity: PersonEntity?, identifier: Pair<EventKeys, String>): List<PersonIdentifierRecord> = when {
    personEntity != PersonEntity.empty -> buildListOfLinkedRecords(personEntity!!, identifier)
    else -> throw PersonRecordNotFoundException(identifier.second)
  }

  private fun buildListOfLinkedRecords(personEntity: PersonEntity, identifier: Pair<EventKeys, String>): List<PersonIdentifierRecord> {
    telemetryService.trackEvent(TelemetryEventType.CPR_SEARCH_REQUEST,
      mapOf(
        identifier.first to identifier.second,
        EventKeys.UUID to personEntity.personKey?.let { it.personId.toString() }
      ))
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
