package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.QueueService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val searchService: SearchService,
  private val queueService: QueueService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
    updatedEntity.personKey?.personId?.let { queueService.publishReclusterMessageToQueue(it) }
    return updatedEntity
  }

  fun linkPersonEntityToPersonKey(personEntity: PersonEntity, personKeyEntity: PersonKeyEntity?) {
    personKeyEntity?.let {
      personEntity.personKey = personKeyEntity
      personRepository.saveAndFlush(personEntity)
    }
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.from(person)
    return personRepository.saveAndFlush(personEntity)
  }

  fun searchBySourceSystem(person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsBySourceSystem(person)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }
}
