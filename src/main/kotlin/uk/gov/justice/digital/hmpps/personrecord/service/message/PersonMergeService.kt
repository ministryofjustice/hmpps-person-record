package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.ClusterMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

/**
 * Merges one PersonEntity into another PersonEntity.
 *
 * This is accomplished by updating the 'mergedTo' column on the FROM PersonEntity
 * to the primary key of the TO PersonEntity. Lastly the FROM PersonEntity is moved from its original cluster to its new cluster (the cluster belonging to the TO PersonEntity).
 *
 * Note: Alongside the primary behavior above, this class also handles some additional work (◞‸◟,).
 * 1. In the event of a from record being merged that belonged to a single node cluster, it will mark the original cluster it belonged to as merged
 * and point to the cluster the TO PersonEntity belongs to.
 * 2. Deletes the FROM person from the 'hmpps-person-match' service.
 * 3. Publishes events (writes to 'event_log' table and posts events to telemetry)
 */
@Component
class PersonMergeService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    if (fromPersonIsPartOfASingleNodeCluster(from)) {
      // TODO: Why not just delete?
      updateFromClusterAsMerged(from, to)
    }
    mergePersons(from, to)
  }

  private fun updateFromClusterAsMerged(from: PersonEntity?, to: PersonEntity) {
    from?.personKey?.let {
      it.throwIfCircularMerge(to.personKey!!)
      it.markAsMerged(to.personKey!!)
      personKeyRepository.save(it)
      publisher.publishEvent(ClusterMerged(from, to, it))
    }
  }

  private fun mergePersons(from: PersonEntity?, to: PersonEntity) {
    val fromClusterDetail = EventLogClusterDetail.from(from?.personKey)
    from?.let {
      it.throwIfCircularMerge(to)
      it.removePersonKeyLink()
      it.mergeTo(to)
      personRepository.save(it)
      personMatchService.deleteFromPersonMatch(it)
    }
    publisher.publishEvent(PersonMerged(from, fromClusterDetail, to))
  }

  private fun PersonKeyEntity.throwIfCircularMerge(to: PersonKeyEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun PersonEntity.throwIfCircularMerge(to: PersonEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun fromPersonIsPartOfASingleNodeCluster(from: PersonEntity?): Boolean = from?.personKey?.hasOneRecord() == true
}
