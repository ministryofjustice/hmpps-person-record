package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION_EXCLUDE
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    personMatchService.saveToPersonMatch(personEntity)
    if (person.linkOnCreate) {
      linkRecordToPersonKey(personEntity)
    }
    return personEntity
  }

  fun updatePersonEntity(person: Person, personEntity: PersonEntity): PersonUpdated {
    val oldMatchingDetails = PersonMatchRecord.from(personEntity)
    updateExistingPersonEntity(person, personEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        personEntity,
      ),
    )
    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(personEntity)
    }
    personEntity.personKey?.let {
      if (person.reclusterOnUpdate && matchingFieldsHaveChanged) {
        reclusterService.recluster(personEntity)
      }
    }
    return PersonUpdated(personEntity, matchingFieldsHaveChanged)
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val matches = personMatchService.findClustersToJoin(personEntity)
    if (matches.containsExcluded().isNotEmpty()) {
      matches.containsExcluded().forEach {
        it.status = NEEDS_ATTENTION_EXCLUDE
        personKeyRepository.save(it)
      }
      personKeyService.assignPersonToNewPersonKey(personEntity)
      personEntity.personKey?.status = NEEDS_ATTENTION_EXCLUDE

      personKeyRepository.save(personEntity.personKey!!)
      return personRepository.saveAndFlush(personEntity)
    }
    if (matches.isEmpty()) {
      personKeyService.assignPersonToNewPersonKey(personEntity)
    } else {
      personKeyService.assignToPersonKeyOfHighestConfidencePerson(personEntity, matches.first().personEntity)
      if (matches.size > 1) {
        reclusterService.recluster(personEntity)
      }
    }

    return personRepository.saveAndFlush(personEntity)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity) {
    personEntity.update(person)
    personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    return personRepository.save(personEntity)
  }
}

private fun List<PersonMatchResult>.containsExcluded(): List<PersonKeyEntity> {
  val allKeys = this.map { it.personEntity.personKey }
  val allEntities = allKeys.flatMap { it?.personEntities!! }
  val allEntityIds = allEntities.map { it.id }
  val allOverrides = allEntities.map { it.overrideMarkers }.flatten().filter { it.markerType == OverrideMarkerType.EXCLUDE }
  return allOverrides.filter { it.markerValue in allEntityIds }.map { it.person?.personKey!! }
}
