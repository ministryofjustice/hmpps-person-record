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
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import java.time.Instant
import java.time.format.DateTimeFormatter

@Profile("!preprod & !prod")
@Component
class AddressDomainEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value("\${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    publishAddressDomainEvent(
      addressEntity = addressCreated.addressEntity,
      eventSource = addressCreated.eventSource.identifier,
      action = "created",
      config = buildAddressDomainEventConfig(addressCreated.addressEntity.person!!.sourceSystem, CPR_PROBATION_ADDRESS_CREATED),
    )
  }

  @TransactionalEventListener
  fun onAddressUpdated(addressUpdated: AddressUpdated) {
    publishAddressDomainEvent(
      addressEntity = addressUpdated.addressEntity,
      eventSource = addressUpdated.eventSource.identifier,
      action = "updated",
      config = buildAddressDomainEventConfig(addressUpdated.addressEntity.person!!.sourceSystem, CPR_PROBATION_ADDRESS_UPDATED),
    )
  }

  private fun publishAddressDomainEvent(
    addressEntity: AddressEntity,
    eventSource: String,
    action: String,
    config: AddressDomainEventConfig?,
  ) {
    config ?: return
    val sourceSystemId = addressEntity.person!!.extractSourceSystemId()!!
    val addressId = addressEntity.updateId
    val detailUrl = "$baseUrl/person/${config.urlPathSegment}/$sourceSystemId/address/$addressId"

    domainEventPublisher.publish(
      DomainEvent(
        eventType = config.eventType,
        description = "A ${config.typeDescription} address has been $action for a person",
        detailUrl = detailUrl,
        occurredAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(Instant.now()),
        additionalInformation = AdditionalInformation(
          cprAddressId = addressId.toString(),
          outboundDeliusAddressId = addressEntity.deliusAddressId?.toString(),
          eventSource = eventSource,
        ),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(config.identifierName, sourceSystemId),
          ),
        ),
      ),
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
