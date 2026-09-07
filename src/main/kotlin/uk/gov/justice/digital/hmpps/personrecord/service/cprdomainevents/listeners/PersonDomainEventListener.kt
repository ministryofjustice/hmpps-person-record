package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.PersonCreatedEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.PersonDeletedEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person.PersonUpdatedEventPublisher

@Component
class PersonDomainEventListener(
  personCreatedEventPublishers: List<PersonCreatedEventPublisher>,
  personUpdatedEventPublishers: List<PersonUpdatedEventPublisher>,
  personDeletedEventPublishers: List<PersonDeletedEventPublisher>,
) {

  private val createPublishersBySourceSystem = personCreatedEventPublishers.associateBy { it.sourceSystemType }
  private val updatePublishersBySourceSystem = personUpdatedEventPublishers.associateBy { it.sourceSystemType }
  private val deletePublishersBySourceSystem = personDeletedEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onPersonCreated(personCreated: PersonCreated) {
    val sourceSystem = personCreated.personEntity.sourceSystem
    createPublishersBySourceSystem[sourceSystem]?.onCreate(personCreated)
  }

  @TransactionalEventListener
  fun onPersonUpdated(personUpdated: PersonUpdated) {
    val sourceSystem = personUpdated.personEntity.sourceSystem
    if (personUpdated.personHasChanged()) {
      updatePublishersBySourceSystem[sourceSystem]?.onUpdate(personUpdated)
    }
  }

  @TransactionalEventListener
  fun onPersonDeleted(personDeleted: PersonDeleted) {
    val sourceSystem = personDeleted.personEntity.sourceSystem
    deletePublishersBySourceSystem[sourceSystem]?.onDelete(personDeleted)
  }
}
