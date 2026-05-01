package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class MergeService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processMerge(from: PersonEntity?, to: PersonEntity) {
    val fromClusterDetail = EventLogClusterDetail.from(from?.personKey)
    when {
      fromClusterHasOneRecord(from) -> deleteSingleRecordCluster(from)
    }
    merge(from, to, fromClusterDetail)
  }

  private fun deleteSingleRecordCluster(from: PersonEntity?) {
    from?.personKey?.let {
      from.removePersonKeyLink()
      personKeyRepository.delete(it)
    }
  }

  private fun merge(from: PersonEntity?, to: PersonEntity, fromClusterDetail: EventLogClusterDetail) {
    from?.let {
      it.throwIfCircularMerge(to)
      it.removePersonKeyLink()
      it.mergeTo(to)
      personRepository.save(it)
      personMatchService.deleteFromPersonMatch(it)
    }
    publisher.publishEvent(PersonMerged(from, fromClusterDetail, to))
  }

  private fun PersonEntity.throwIfCircularMerge(to: PersonEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun fromClusterHasOneRecord(from: PersonEntity?): Boolean = from?.personKey?.hasOneRecord() == true
}
