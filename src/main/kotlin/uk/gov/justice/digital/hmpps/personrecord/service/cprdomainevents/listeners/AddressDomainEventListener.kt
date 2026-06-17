package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.UK_ZONE
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import java.time.Instant
import java.time.format.DateTimeFormatter

@Profile("!preprod & !prod")
@Component
class AddressDomainEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    publishAddressDomainEvent(
      addressEntity = addressCreated.addressEntity,
      sourceSystemId = addressCreated.addressEntity.person!!.extractSourceSystemId()!!,
      eventSource = addressCreated.eventSource.identifier,
      action = "created",
      config = buildAddressDomainEventConfig(
        sourceSystem = addressCreated.addressEntity.person!!.sourceSystem,
        eventType = CPR_PROBATION_ADDRESS_CREATED,
      ),
    )
  }

  @TransactionalEventListener
  fun onAddressUpdated(addressUpdated: AddressUpdated) {
    publishAddressDomainEvent(
      addressEntity = addressUpdated.addressEntity,
      sourceSystemId = addressUpdated.addressEntity.person!!.extractSourceSystemId()!!,
      eventSource = addressUpdated.eventSource.identifier,
      action = "updated",
      config = buildAddressDomainEventConfig(
        sourceSystem = addressUpdated.addressEntity.person!!.sourceSystem,
        eventType = CPR_PROBATION_ADDRESS_UPDATED,
      ),
    )
  }

  @TransactionalEventListener
  fun onAddressDeleted(addressDeleted: AddressDeleted) {
    publishAddressDomainEvent(
      addressEntity = addressDeleted.addressEntity,
      sourceSystemId = addressDeleted.personEntity.extractSourceSystemId()!!,
      eventSource = addressDeleted.eventSource.identifier,
      action = "deleted",
      config = buildAddressDomainEventConfig(addressDeleted.personEntity.sourceSystem, CPR_PROBATION_ADDRESS_DELETED),
      shouldCallback = false,
    )
  }

  private fun publishAddressDomainEvent(
    addressEntity: AddressEntity,
    sourceSystemId: String,
    eventSource: String,
    action: String,
    config: AddressDomainEventConfig?,
    shouldCallback: Boolean = true,
  ) {
    config ?: return
    val addressId = addressEntity.updateId

    val domainEvent = DomainEvent(
      eventType = config.eventType,
      description = "A ${config.typeDescription} address has been $action for a person",
      occurredAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(Instant.now()),
      additionalInformation = AdditionalInformation(
        outboundCprAddressId = addressId.toString(),
        outboundDeliusAddressId = addressEntity.deliusAddressId,
      ),
      personReference = PersonReference(
        identifiers = listOf(
          PersonIdentifier(config.identifierName, sourceSystemId),
        ),
      ),
    )
    if (shouldCallback) {
      domainEvent.detailUrl = "$baseUrl/person/${config.urlPathSegment}/$sourceSystemId/address/$addressId"
    }
    domainEventPublisher.publish(
      domainEvent = domainEvent,
      attributes = mapOf("eventSource" to eventSource),
    )
  }

  private fun buildAddressDomainEventConfig(sourceSystem: SourceSystemType, eventType: String): AddressDomainEventConfig? = when (sourceSystem) {
    DELIUS -> AddressDomainEventConfig(eventType, "CRN", "probation", "probation")
    else -> null // temporary flow for other source systems we haven't mapped yet
  }
}

data class AddressDomainEventConfig(
  val eventType: String,
  val identifierName: String,
  val urlPathSegment: String,
  val typeDescription: String,
)
