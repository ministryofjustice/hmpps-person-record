package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Component
class PersonKeyService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val telemetryService: TelemetryService,
) {

  fun getOrCreatePersonKey(personEntity: PersonEntity): PersonKeyEntity {
    val highConfidenceRecord: PersonEntity? = personMatchService.findHighestConfidencePersonRecord(personEntity)
    return when {
      highConfidenceRecord == PersonEntity.empty -> createPersonKey(personEntity)
      else -> retrievePersonKey(personEntity, highConfidenceRecord)
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

  private fun retrievePersonKey(personEntity: PersonEntity, highConfidenceRecord: PersonEntity): PersonKeyEntity {
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      personEntity,
      mapOf(
        EventKeys.UUID to highConfidenceRecord.personKey?.personId?.toString(),
        EventKeys.CLUSTER_SIZE to highConfidenceRecord.personKey?.personEntities?.size.toString(),
      ),
    )
    return highConfidenceRecord.personKey!!
  }
}
