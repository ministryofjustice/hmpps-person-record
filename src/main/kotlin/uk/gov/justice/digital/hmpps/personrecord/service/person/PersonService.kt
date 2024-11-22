package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideMarkerEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
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

  fun linkPersonEntityToPersonKey(personEntity: PersonEntity, personKeyEntity: PersonKeyEntity) {
    personEntity.personKey = personKeyEntity
    personRepository.saveAndFlush(personEntity)
  }

  fun removePersonKeyLink(personEntity: PersonEntity) {
    personEntity.personKey.let {
      personEntity.personKey?.personEntities?.remove(personEntity)
      personEntity.personKey = null
      personRepository.save(personEntity)
    }
  }

  fun addExcludeOverrideMarker(personEntity: PersonEntity, excludingRecord: PersonEntity) {
    personEntity.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = excludingRecord.id, person = personEntity),
    )
    personRepository.save(personEntity)
  }

  fun removeMergedLink(personEntity: PersonEntity) {
    personEntity.mergedTo = null
    personRepository.save(personEntity)
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
