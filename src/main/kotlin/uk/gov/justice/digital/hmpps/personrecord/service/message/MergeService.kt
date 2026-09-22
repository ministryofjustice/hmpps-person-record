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

  fun processMerge(fromPersonEntity: PersonEntity, toPersonEntity: PersonEntity, person: Person) {
    val personChangeChecker = PersonChangeChecker(toPersonEntity)
    toPersonEntity.updatePersonEntity(person)
    personRepository.save(toPersonEntity)

    if (personChangeChecker.matchingFieldsHaveChanged(toPersonEntity) && !toPersonEntity.isPassive()) {
      personMatchService.saveToPersonMatch(toPersonEntity)
    }

    publisher.publishEvent(PersonUpdated(toPersonEntity, personChangeChecker))

    val fromClusterDetail = EventLogClusterDetail.from(fromPersonEntity.personKey)
    when {
      fromClusterHasOneRecord(fromPersonEntity) -> deleteSingleRecordCluster(fromPersonEntity)
    }
    merge(fromPersonEntity, toPersonEntity, fromClusterDetail)
  }

  private fun deleteSingleRecordCluster(from: PersonEntity?) {
    from?.personKey?.let {
      from.removePersonKeyLink()
      personKeyDeletionService.deletePersonKey(it, from)
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
