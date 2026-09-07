package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_DELETED
import java.time.Instant

@Component
class ProbationPersonDeletedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonDeletedEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onDelete(personDeleted: PersonDeleted) {
    val personEntity = personDeleted.personEntity
    val crn = personEntity.extractSourceSystemId()!!

    domainEventPublisher.publish(
      domainEvent = CprPersonDeleted(
        eventType = CPR_PROBATION_PERSON_DELETED,
        occurredAt = Instant.now().asStringWithUkZone(),
        description = "A probation person record has been deleted",
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
    )
  }
}
