package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val searchService: SearchService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity {
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
    return updatedEntity
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personKeyEntity = personKeyService.getPersonKey(personEntity)
    personEntity.personKey = personKeyEntity
    return personRepository.save(personEntity)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.from(person)
    return personRepository.save(personEntity)
  }

  fun searchBySourceSystem(person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsBySourceSystem(person)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }
}
