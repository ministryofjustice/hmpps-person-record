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
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import java.time.Instant

@Component
class PrisonPersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher {
  override val sourceSystemType = SourceSystemType.NOMIS

  override fun onCreate(personCreated: PersonCreated) {
    publishPersonDomainEvent(
      personCreated.personEntity,
      CPR_PRISON_PERSON_CREATED,
    )
  }

  override fun onUpdate(personUpdated: PersonUpdated): Unit = throw NotImplementedError("Person update events are not currently published for prison records")

  override fun onDelete(personDeleted: PersonDeleted): Unit = throw NotImplementedError("Person delete events are not currently published for prison records")

  private fun publishPersonDomainEvent(
    personEntity: PersonEntity,
    eventType: String,
  ) {
    val noms = personEntity.extractSourceSystemId()!!
    val sourceSystemId = personEntity.extractSourceSystemId()
    val detailUrl = "$baseUrl/person/prison/$sourceSystemId"

    domainEventPublisher.publish(
      DomainEvent(
        eventType = eventType,
        description = "A prison person record has been created",
        detailUrl = detailUrl,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("NOMS", noms),
          ),
        ),
      ),
    )
  }
}
