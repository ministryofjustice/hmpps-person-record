package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.contact.ContactUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.ContactEventPublisher

@Component
class ContactEventListener(contactEventPublishers: List<ContactEventPublisher>) {
  private val publishersBySourceSystem = contactEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onContactCreated(contactCreated: ContactCreated) {
    publishersBySourceSystem[contactCreated.sourceSystemType]?.onCreate(contactCreated)
  }

  @TransactionalEventListener
  fun onContactUpdated(contactUpdated: ContactUpdated) {
    publishersBySourceSystem[contactUpdated.sourceSystemType]?.onUpdate(contactUpdated)
  }
}
