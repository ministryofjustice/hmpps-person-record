package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated

@Component
class PersonLogProcessor(
  private val publisher: ApplicationEventPublisher,
) {

  fun logCreate(personEntity: PersonEntity) = publisher.publishEvent(PersonCreated(personEntity))

  fun logUpdate(personEntity: PersonEntity) = publisher.publishEvent(PersonUpdated(personEntity, shouldRecluster = false))
}