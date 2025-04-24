package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.ClusterMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged

@Component
class NewMergeService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val deletionService: DeletionService,
  private val publisher: ApplicationEventPublisher,
) {

  @Transactional
  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    when {
      fromClusterHasOneRecord(from) -> markClusterAsMerged(from, to)
    }
    merge(from, to)
  }

  private fun markClusterAsMerged(from: PersonEntity?, to: PersonEntity) {
    from?.personKey?.let {
      it.markAsMerged(to.personKey!!)
      personKeyRepository.save(it)
      publisher.publishEvent(ClusterMerged(from, to, it))
    }
  }

  private fun merge(from: PersonEntity?, to: PersonEntity) {
    publisher.publishEvent(PersonMerged(from, to))
    from?.let {
      it.removePersonKeyLink()
      it.mergeTo(to)
      personRepository.save(it)
      deletionService.deletePersonFromPersonMatch(it)
    }
  }

  private fun fromClusterHasOneRecord(from: PersonEntity?): Boolean = (from?.personKey?.personEntities?.size ?: 0) == 1
}
