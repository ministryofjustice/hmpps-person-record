package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personKeyRepository: PersonKeyRepository,
  private val personFactory: PersonFactory,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun handlePersonCreation(person: Person): PersonEntity {
    val personEntity = personFactory.create(person)
    personMatchService.saveToPersonMatch(personEntity)
    if (person.linkOnCreate) {
      linkRecordToPersonKey(personEntity)
    }
    publisher.publishEvent(PersonCreated(personEntity))
    return personEntity
  }

  fun handlePersonUpdate(person: Person, personEntity: PersonEntity): PersonEntity {
    val oldMatchingDetails = PersonMatchRecord.from(personEntity)
    personFactory.update(person, personEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        personEntity,
      ),
    )
    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(personEntity)
    }
    publisher.publishEvent(PersonUpdated(personEntity, matchingFieldsHaveChanged))
    personEntity.personKey?.let {
      if (person.reclusterOnUpdate && matchingFieldsHaveChanged) {
        reclusterService.recluster(personEntity)
      }
    }
    return personEntity
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity) {
    personKeyService.assignPersonToNewPersonKey(personEntity)
    val matches = personMatchService.findClustersToJoin(personEntity)
    if (matches.isNotEmpty()) {
      reclusterService.recluster(personEntity)
    }
  }
}

private fun List<PersonMatchResult>.containsExcluded(): List<PersonKeyEntity> {
  val allKeys = this.map { it.personEntity.personKey }
  val allEntities = allKeys.flatMap { it?.personEntities!! }
  val allEntityIds = allEntities.map { it.id }
  val allOverrides = allEntities.map { it.overrideMarkers }.flatten().filter { it.markerType == OverrideMarkerType.EXCLUDE }
  return allOverrides.filter { it.markerValue in allEntityIds }.map { it.person?.personKey!! }
}
