package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.CircularMergeException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.PersonChangeChecker
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyDeletionService
import uk.gov.justice.digital.hmpps.personrecord.service.person.updatePersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import kotlin.reflect.KClass

@Component
class MergeService(
  private val personRepository: PersonRepository,
  private val reclusterService: ReclusterService,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
  private val personKeyDeletionService: PersonKeyDeletionService,
) {

  fun processMerge(from: PersonEntity, to: PersonEntity, person: Person, childrenToIgnore: Set<KClass<*>> = emptySet()) {
    val personChangeChecker = PersonChangeChecker(to)
    updateToPerson(to, person, personChangeChecker, childrenToIgnore)
    val fromClusterDetail = EventLogClusterDetail.from(from.personKey)
    when {
      fromClusterHasOneRecord(from) -> deleteSingleRecordCluster(from)
    }
    merge(from, to, fromClusterDetail, personChangeChecker)
    reclusterService.recluster(to)
  }

  private fun updateToPerson(to: PersonEntity, person: Person, personChangeChecker: PersonChangeChecker, childrenToIgnore: Set<KClass<*>> = emptySet()) {
    to.updatePersonEntity(person, childrenToIgnore)
    personRepository.save(to)

    if (personChangeChecker.shouldSaveToPersonMatch(to)) {
      personMatchService.saveToPersonMatch(to)
    }
  }

  private fun deleteSingleRecordCluster(from: PersonEntity) {
    val personKeyEntity = from.personKey!!
    from.removePersonKeyLink()
    personKeyDeletionService.deletePersonKey(personKeyEntity, from)
  }

  private fun merge(
    from: PersonEntity,
    to: PersonEntity,
    fromClusterDetail: EventLogClusterDetail,
    personChangeChecker: PersonChangeChecker,
  ) {
    from.throwIfCircularMerge(to)
    from.removePersonKeyLink()
    from.mergeTo(to)
    personRepository.save(from)
    personMatchService.deleteFromPersonMatch(from)
    publisher.publishEvent(PersonMerged(from, fromClusterDetail, to, personChangeChecker))
  }

  private fun PersonEntity.throwIfCircularMerge(to: PersonEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun fromClusterHasOneRecord(from: PersonEntity): Boolean = from.personKey?.hasOneRecord() == true
}
