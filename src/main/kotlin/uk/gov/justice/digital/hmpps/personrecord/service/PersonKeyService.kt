package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Service
class PersonKeyService(
  private val searchService: SearchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryService: TelemetryService,
) {

  fun setPersonKeyStatus(personKeyEntity: PersonKeyEntity, status: UUIDStatusType) {
    personKeyEntity.status = status
    personKeyRepository.saveAndFlush(personKeyEntity)
  }

  fun getPersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val highConfidenceRecordWithUuid = searchByAllSourceSystemsAndHasUuid(personEntity)
    return when {
      highConfidenceRecordWithUuid == null -> createPersonKey(personEntity)
      else -> retrievePersonKey(personEntity, highConfidenceRecordWithUuid)
    }
  }

  private fun createPersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val personKey = PersonKeyEntity.new()
    trackEvent(
      CPR_UUID_CREATED,
      personEntity,
      mapOf(EventKeys.UUID to personKey.personId.toString()),
    )
    return personKeyRepository.saveAndFlush(personKey)
  }

  private fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecordWithUuid: PersonEntity): PersonKeyEntity {
    trackEvent(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      personEntity,
      mapOf(EventKeys.UUID to highConfidenceRecordWithUuid.personKey?.personId?.toString()),
    )
    return highConfidenceRecordWithUuid.personKey!!
  }

  private fun searchByAllSourceSystemsAndHasUuid(personEntity: PersonEntity): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsWithUuid(personEntity)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    personEntity: PersonEntity,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to personEntity.sourceSystem.name,
      EventKeys.DEFENDANT_ID to personEntity.defendantId,
      EventKeys.CRN to personEntity.crn,
      EventKeys.PRISON_NUMBER to personEntity.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }
}
