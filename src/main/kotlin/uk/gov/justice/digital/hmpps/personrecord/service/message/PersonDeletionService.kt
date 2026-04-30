package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
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
  private val reviewRepository: ReviewRepository,
) {

  @Transactional
  fun processDelete(personCallback: () -> PersonEntity?) = personCallback()?.let { personEntity ->
    val personsOnCluster = personEntity.personKey?.personEntities?.filter { it.matchId != personEntity.matchId }
    personEntity.deleteClusterIfNoRecordsLeft()
    personEntity.delete()
    personEntity.deleteFromPersonMatch()
    personEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively()
    reclusterRemainingPeopleInCluster(personsOnCluster)
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
    val findByClustersPersonKey = reviewRepository.findByClustersPersonKey(personKeyEntity)
    findByClustersPersonKey?.let { review ->
      reviewRepository.delete(review)
    }
    personKeyRepository.delete(personKeyEntity)
    publisher.publishEvent(PersonKeyDeleted(personEntity, personKeyEntity))
  }

  private fun PersonEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively() {
    personRepository.findByMergedTo(this.id!!).filterNotNull().forEach { personEntity ->
      personEntity.delete()
      personEntity.deletePersonEntityThatWasMergedIntoThisOneRecursively()
    }
  }

  private fun reclusterRemainingPeopleInCluster(personsOnCluster: List<PersonEntity>?) {
    personsOnCluster?.forEach { reclusterService.recluster(it) }
  }
}
