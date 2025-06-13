package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.ClusterMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class MergeService(
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    when {
      fromClusterHasOneRecord(from) -> markClusterAsMerged(from, to)
    }
    merge(from, to)
  }

  private fun markClusterAsMerged(from: PersonEntity?, to: PersonEntity) {
    from?.personKey?.let {
      it.throwIfCircularMerge(to.personKey!!)
      it.markAsMerged(to.personKey!!)
      publisher.publishEvent(ClusterMerged(from, to, it))
    }
  }

  private fun merge(from: PersonEntity?, to: PersonEntity) {
    publisher.publishEvent(PersonMerged(from, to))
    from?.let {
      it.throwIfCircularMerge(to)
      it.removePersonKeyLink()
      it.mergeTo(to)
      personMatchService.deleteFromPersonMatch(it)
    }
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

  private fun fromClusterHasOneRecord(from: PersonEntity?): Boolean = (from?.personKey?.personEntities?.size ?: 0) == 1
}
