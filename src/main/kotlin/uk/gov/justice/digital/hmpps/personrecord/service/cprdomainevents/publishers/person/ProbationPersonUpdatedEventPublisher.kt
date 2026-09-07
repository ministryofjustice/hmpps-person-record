package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED
import java.time.Instant

@Component
class ProbationPersonUpdatedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonUpdatedEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onUpdate(personUpdated: PersonUpdated) {
    val crn = personUpdated.personEntity.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprPersonUpdated(
        eventType = CPR_PROBATION_PERSON_UPDATED,
        description = "A probation person record has been updated",
        detailUrl = "$baseUrl/person/probation/$crn",
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
    )
  }
}
