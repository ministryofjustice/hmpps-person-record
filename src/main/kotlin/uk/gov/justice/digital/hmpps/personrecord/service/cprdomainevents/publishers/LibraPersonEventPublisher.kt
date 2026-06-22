package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import java.time.Instant

@Component
class LibraPersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher {
  override val sourceSystemType = SourceSystemType.LIBRA

  override fun onCreate(personCreated: PersonCreated) {
    publishPersonDomainEvent(
      personCreated.personEntity,
      CPR_COURT_PERSON_CREATED,
    )
  }

  private fun publishPersonDomainEvent(
    personEntity: PersonEntity,
    eventType: String,
  ) {
    val cId = personEntity.extractSourceSystemId()!!
    val sourceSystemId = personEntity.extractSourceSystemId()
    val detailUrl = "$baseUrl/person/libra/$sourceSystemId"

    domainEventPublisher.publish(
      DomainEvent(
        eventType = eventType,
        description = "A court person record has been created",
        detailUrl = detailUrl,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("C_ID", cId),
          ),
        ),
      ),
    )
  }
}
