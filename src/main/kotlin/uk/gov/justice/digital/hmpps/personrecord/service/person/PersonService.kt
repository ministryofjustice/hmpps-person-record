package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val eventPublisher: ApplicationEventPublisher,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity, shouldReclusterOnUpdate: Boolean): PersonUpdated {
    val oldMatchingDetails = PersonMatchRecord.from(existingPersonEntity)
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    personMatchService.saveToPersonMatch(updatedEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        updatedEntity,
      ),
    )

    val shouldRecluster = when (clusterNeedsAttentionAndIsInvalid(existingPersonEntity.personKey!!)) {
      true -> false
      else -> shouldReclusterOnUpdate
    }
    settingNeedsAttentionClusterToActive(updatedEntity.personKey!!, updatedEntity)
    return PersonUpdated(updatedEntity, matchingFieldsHaveChanged, shouldRecluster)
  }

  private fun settingNeedsAttentionClusterToActive(personKeyEntity: PersonKeyEntity, changedRecord: PersonEntity) {
    if (personKeyEntity.isNeedsAttention()) {
      personKeyEntity.status = ACTIVE
      personKeyRepository.save(personKeyEntity)
      eventPublisher.publishEvent(
        RecordEventLog(
          CPRLogEvents.CPR_NEEDS_ATTENTION_TO_ACTIVE,
          changedRecord,
          personKeyEntity,
        ),
      )
    }
  }
  private fun clusterNeedsAttentionAndIsInvalid(cluster: PersonKeyEntity) = cluster.isNeedsAttention() && !personMatchService.examineIsClusterValid(cluster).isClusterValid
  private fun PersonKeyEntity.isNeedsAttention(): Boolean = this.status == NEEDS_ATTENTION

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personEntityWithKey = personMatchService.findHighestConfidencePersonRecord(personEntity).exists(
      no = { personKeyService.createPersonKey(personEntity) },
      yes = { personKeyService.retrievePersonKey(personEntity, it) },
    )
    return personRepository.saveAndFlush(personEntityWithKey)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    return personRepository.save(personEntity)
  }
}
