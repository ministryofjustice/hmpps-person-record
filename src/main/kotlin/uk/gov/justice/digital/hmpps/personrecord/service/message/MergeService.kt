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
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyDeletionService
import uk.gov.justice.digital.hmpps.personrecord.service.person.updatePersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class MergeService(
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val publisher: ApplicationEventPublisher,
  private val personKeyDeletionService: PersonKeyDeletionService,
) {

  fun processMerge(from: PersonEntity, to: PersonEntity, person: Person) {
    updateToPerson(to, person)

    val fromClusterDetail = EventLogClusterDetail.from(from.personKey)
    when {
      fromClusterHasOneRecord(from) -> deleteSingleRecordCluster(from)
    }
    merge(from, to, fromClusterDetail)
  }

  private fun updateToPerson(to: PersonEntity, person: Person) {
    val personChangeChecker = PersonChangeChecker(to)
    to.updatePersonEntity(person)
    personRepository.save(to)

    if (personChangeChecker.matchingFieldsHaveChanged(to) && !to.isPassive()) {
      personMatchService.saveToPersonMatch(to)
    }
    publisher.publishEvent(PersonUpdated(to, personChangeChecker))
  }

  private fun deleteSingleRecordCluster(from: PersonEntity) {
    val personKeyEntity = from.personKey!!
    from.removePersonKeyLink()
    personKeyDeletionService.deletePersonKey(personKeyEntity, from)
  }

  private fun merge(from: PersonEntity, to: PersonEntity, fromClusterDetail: EventLogClusterDetail) {
    from.throwIfCircularMerge(to)
    from.removePersonKeyLink()
    from.mergeTo(to)
    personRepository.save(from)
    personMatchService.deleteFromPersonMatch(from)
    publisher.publishEvent(PersonMerged(from, fromClusterDetail, to))
  }

  private fun PersonEntity.throwIfCircularMerge(to: PersonEntity) {
    if (to.mergedTo == this.id) {
      throw CircularMergeException()
    }
  }

  private fun fromClusterHasOneRecord(from: PersonEntity): Boolean = from.personKey?.hasOneRecord() == true
}
