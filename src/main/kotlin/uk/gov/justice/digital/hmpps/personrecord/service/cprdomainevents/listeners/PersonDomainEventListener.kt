package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.PersonEventPublisher

@Profile("!preprod & !prod")
@Component
class PersonDomainEventListener(personEventPublishers: List<PersonEventPublisher>) {

  private val publishersBySourceSystem = personEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onPersonCreated(personCreated: PersonCreated) {
    val sourceSystem = personCreated.personEntity.sourceSystem
    publishersBySourceSystem[sourceSystem]?.onCreate(personCreated)
  }
}