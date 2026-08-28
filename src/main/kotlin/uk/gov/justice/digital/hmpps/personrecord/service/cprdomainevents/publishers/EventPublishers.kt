package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import java.time.Instant

abstract class PersonEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {
  abstract val sourceSystemType: SourceSystemType
  abstract val sourceSystemIdField: String
  abstract val path: String

  abstract val createEventType: String
  abstract val updateEventType: String
  abstract val createdDescription: String
  abstract val updatedDescription: String

  fun onCreate(personCreated: PersonCreated) {
    val personEntity = personCreated.personEntity
    val sourceSystemId = personEntity.extractSourceSystemId()!!
    val detailUrl = "$baseUrl$path$sourceSystemId"

    domainEventPublisher.publish(
      CprPersonCreated(
        eventType = createEventType,
        description = createdDescription,
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

  fun onUpdate(personUpdated: PersonUpdated) {
    val sourceSystemId = personUpdated.personEntity.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprPersonUpdated(
        eventType = updateEventType,
        description = updatedDescription,
        detailUrl = "$baseUrl$path$sourceSystemId",
        occurredAt = Instant.now().asStringWithUkZone(),
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
