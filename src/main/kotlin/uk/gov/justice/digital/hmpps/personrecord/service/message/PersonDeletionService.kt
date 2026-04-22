package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
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
  fun processDelete(personEntity: PersonEntity) {
    deletePerson(personEntity)
  }

  private fun deletePerson(personEntity: PersonEntity) {
    val cluster = personEntity.personKey
    personEntity.deleteClusterIfNoRecordsLeft()
    personEntity.delete(cluster)
    personEntity.deleteFromPersonMatch()
    personEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively()
  }

  private fun PersonEntity.deleteClusterIfNoRecordsLeft() {
    val cluster = this.personKey
    when {
      cluster?.hasOneRecord() == true -> deletePersonKey(cluster, this)
      else -> {
        this.removePersonKeyLink()
        if (this.mergedTo == null) {
          personKeyRepository.save(cluster!!)
        }
      }
    }
  }

  private fun PersonEntity.delete(cluster: PersonKeyEntity?) {
    personRepository.delete(this)
    publisher.publishEvent(PersonDeleted(this, cluster))
  }

  private fun PersonEntity.deleteFromPersonMatch() {
    personMatchService.deleteFromPersonMatch(this)
  }

  private fun deletePersonKey(personKeyEntity: PersonKeyEntity, personEntity: PersonEntity) {
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun PersonEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively() {
    personRepository.findByMergedTo(this.id!!).forEach { personEntity -> personEntity?.let { deletePerson(it) } }
  }
}
