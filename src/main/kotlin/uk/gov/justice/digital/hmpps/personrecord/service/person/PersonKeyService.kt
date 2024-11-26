package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Component
class PersonKeyService(
  private val searchService: SearchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryService: TelemetryService,
) {

  fun getPersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val highConfidenceRecordWithUuid = searchByAllSourceSystemsAndHasUuid(personEntity)
    return when {
      highConfidenceRecordWithUuid == null -> createPersonKey(personEntity)
      else -> retrievePersonKey(personEntity, highConfidenceRecordWithUuid)
    }
  }

  private fun createPersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val personKey = PersonKeyEntity.new()
    telemetryService.trackPersonEvent(
      CPR_UUID_CREATED,
      personEntity,
      mapOf(EventKeys.UUID to personKey.personId.toString()),
    )
    return personKeyRepository.save(personKey)
  }

  private fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecordWithUuid: PersonEntity): PersonKeyEntity {
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      personEntity,
      mapOf(
        EventKeys.UUID to highConfidenceRecordWithUuid.personKey?.personId?.toString(),
        EventKeys.CLUSTER_SIZE to highConfidenceRecordWithUuid.personKey?.personEntities?.size.toString(),
      ),
    )
    return highConfidenceRecordWithUuid.personKey!!
  }

  private fun searchByAllSourceSystemsAndHasUuid(personEntity: PersonEntity): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsWithUuid(personEntity)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }
}
