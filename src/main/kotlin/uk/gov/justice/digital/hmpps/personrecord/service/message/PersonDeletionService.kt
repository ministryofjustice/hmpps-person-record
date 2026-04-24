package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonDeletionService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
  private val reclusterService: ReclusterService,
) {

  @Transactional
  fun processDelete(personCallback: () -> PersonEntity?) = personCallback()?.let { personEntity ->
    val cluster = personEntity.personKey
    deletePerson(personEntity)
    reclusterRemainingPeopleInCluster(personEntity, cluster)
  }

  private fun deletePerson(personEntity: PersonEntity) {
    personEntity.deleteClusterIfNoRecordsLeft()
    personEntity.delete()
    personEntity.deleteFromPersonMatch()
    personEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively()
  }

  private fun PersonEntity.deleteClusterIfNoRecordsLeft() {
    if (this.personKey?.hasOneRecord() == true) {
      deletePersonKey(this.personKey!!, this)
    }
  }

  private fun PersonEntity.delete() {
    val cluster = this.personKey
    this.removePersonKeyLink()
    personRepository.delete(this)
    publisher.publishEvent(PersonDeleted(this, cluster))
  }

  private fun PersonEntity.deleteFromPersonMatch() = personMatchService.deleteFromPersonMatch(this)

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun PersonEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively() {
    personRepository.findByMergedTo(this.id!!).filterNotNull().forEach { personEntity -> deletePerson(personEntity) }
  }

  private fun reclusterRemainingPeopleInCluster(deletedPersonEntity: PersonEntity, cluster: PersonKeyEntity?) {
    val remainingPersonsInCluster = cluster?.personEntities?.filter { it.mergedTo == null && deletedPersonEntity.id != it.id } ?: emptyList()
    remainingPersonsInCluster.forEach { nonMergedPersonEntity -> reclusterService.recluster(nonMergedPersonEntity) }
  }
}
