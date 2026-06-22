package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import java.time.Instant

abstract class PersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {
  abstract val sourceSystemType: SourceSystemType
  abstract val identifierType: String
  abstract fun onCreate(personCreated: PersonCreated)

  protected fun publishPersonDomainEvent(personEntity: PersonEntity, eventType: String) {
    val sourceSystemId = personEntity.extractSourceSystemId()!!
    val detailUrl = "$baseUrl/person/${sourceSystemType.description}/$sourceSystemId"
    domainEventPublisher.publish(
      DomainEvent(
        eventType = eventType,
        description = "A ${sourceSystemType.description} person record has been created",
        detailUrl = detailUrl,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(identifierType, sourceSystemId),
          ),
        ),
      ),
    )
  }
}
