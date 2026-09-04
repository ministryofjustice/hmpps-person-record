package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.PersonEventPublisher

@Component
class PersonDomainEventListener(personEventPublishers: List<PersonEventPublisher>) {

  private val publishersBySourceSystem = personEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onPersonCreated(personCreated: PersonCreated) {
    val sourceSystem = personCreated.personEntity.sourceSystem
    publishersBySourceSystem[sourceSystem]?.onCreate(personCreated)
  }

  @TransactionalEventListener
  fun onPersonUpdated(personUpdated: PersonUpdated) {
    val sourceSystem = personUpdated.personEntity.sourceSystem
    if (personUpdated.personHasChanged()) {
      publishersBySourceSystem[sourceSystem]?.onUpdate(personUpdated)
    }
  }

  @TransactionalEventListener
  fun onPersonDeleted(personDeleted: PersonDeleted) {
    val sourceSystem = personDeleted.personEntity.sourceSystem
    publishersBySourceSystem[sourceSystem]?.onDelete(personDeleted)
  }
}
