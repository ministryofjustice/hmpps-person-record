package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonDeletionService(
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun processDelete(personCallback: () -> PersonEntity?) = personCallback()?.let { personEntity ->
    personEntity.deleteClusterIfNoRecordsLeft()
    personEntity.delete()
    personEntity.deleteFromPersonMatchIfStillExists()
    personEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively()
  } ?: throw ResourceNotFoundException("Person does not exist")

  private fun PersonEntity.deleteClusterIfNoRecordsLeft() {
    this.personKey?.let { cluster ->
      when {
        cluster.hasOneRecord() -> deletePersonKey(cluster, this)
        else -> removeLinkToRecord(cluster, this)
      }
    }
  }

  private fun PersonEntity.delete() {
    personRepository.delete(this)
    publisher.publishEvent(PersonDeleted(this))
  }

  private fun PersonEntity.deleteFromPersonMatchIfStillExists() {
    if (this.mergedTo == null) {
      personMatchService.deleteFromPersonMatch(this)
    }
  }

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun removeLinkToRecord(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyEntity.personEntities.remove(personEntity)
    personKeyRepository.save(personKeyEntity)
  }

  private fun PersonEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively() {
    personRepository.findByMergedTo(this.id!!).forEach { personEntity -> processDelete { personEntity } }
  }
}
