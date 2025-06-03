package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

@Component
class PersonLogProcessor(
  private val publisher: ApplicationEventPublisher,
) {

  fun logCreate(context: PersonContext) = context.personEntity?.let { publisher.publishEvent(PersonCreated(it)) }

  fun logUpdate(context: PersonContext) = context.personEntity?.let {
    publisher.publishEvent(PersonUpdated(it, matchingFieldsHaveChanged = context.hasMatchingFieldsChanged, shouldRecluster = false))
  }
}
