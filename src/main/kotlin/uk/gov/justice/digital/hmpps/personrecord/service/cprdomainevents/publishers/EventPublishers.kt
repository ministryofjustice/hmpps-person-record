package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import java.time.Instant

abstract class PersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {
  abstract val sourceSystemType: SourceSystemType
  abstract val createEventType: String
  abstract val deleteEventType: String
  abstract val path: String
  abstract val createDescription: String
  abstract val deleteDescription: String
  abstract val sourceSystemIdField: String

  fun onCreate(personCreated: PersonCreated) {
    val personEntity = personCreated.personEntity
    val sourceSystemId = personEntity.extractSourceSystemId()!!
    val detailUrl = "$baseUrl$path$sourceSystemId"

    domainEventPublisher.publish(
      CprPersonCreated(
        eventType = createEventType,
        description = createDescription,
        detailUrl = detailUrl,
        occurredAt = Instant.now().asStringWithUkZone(),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(sourceSystemIdField, sourceSystemId),
          ),
        ),
      ),
    )
  }

  fun onDelete(personDeleted: PersonDeleted) {
    val personEntity = personDeleted.personEntity
    val sourceSystemId = personEntity.extractSourceSystemId()!!

    domainEventPublisher.publish(
      domainEvent = CprPersonDeleted(
        eventType = deleteEventType,
        occurredAt = Instant.now().asStringWithUkZone(),
        description = deleteDescription,
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(sourceSystemIdField, sourceSystemId),
          ),
        ),
      ),
    )
  }
}

interface ReligionEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(religionCreated: ReligionCreated)
  fun onUpdate(religionUpdated: ReligionUpdated)
}

interface AddressEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(addressCreated: AddressCreated)
  fun onUpdate(addressUpdated: AddressUpdated)
  fun onDelete(addressDeleted: AddressDeleted)
}
