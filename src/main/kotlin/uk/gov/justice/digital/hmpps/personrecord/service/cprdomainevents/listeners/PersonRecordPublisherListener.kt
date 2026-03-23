package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PublishPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PublishPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PublishPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.PersonRecordPublisher

@Component
class PersonRecordPublisherListener(
  private val personRecordPublisher: PersonRecordPublisher,
) {

  @EventListener
  fun onPersonCreatedForDomainEvent(event: PublishPersonCreated) {
    personRecordPublisher.publishPersonCreated(event.personEntity)
  }

  @EventListener
  fun onPersonUpdatedForDomainEvent(event: PublishPersonUpdated) {
    personRecordPublisher.publishPersonUpdated(event.personEntity)
  }

  @EventListener
  fun onPersonDeletedForDomainEvent(event: PublishPersonDeleted) {
    personRecordPublisher.publishPersonDeleted(event.personEntity)
  }
}
