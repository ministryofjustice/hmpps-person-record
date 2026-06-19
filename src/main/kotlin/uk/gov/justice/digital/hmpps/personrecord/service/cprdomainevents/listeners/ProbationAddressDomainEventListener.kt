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
class ProbationAddressDomainEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    val sourceSystem = addressCreated.addressEntity.person!!.sourceSystem
    if (sourceSystem == DELIUS) {
      publishProbationAddressDomainEvent(
        addressEntity = addressCreated.addressEntity,
        eventSource = addressCreated.eventSource.identifier,
        action = "created",
        eventType = CPR_PROBATION_ADDRESS_CREATED,
      )
    }
  }

  @TransactionalEventListener
  fun onAddressUpdated(addressUpdated: AddressUpdated) {
    val sourceSystem = addressUpdated.addressEntity.person!!.sourceSystem
    if (sourceSystem == DELIUS) {
      publishProbationAddressDomainEvent(
        addressEntity = addressUpdated.addressEntity,
        eventSource = addressUpdated.eventSource.identifier,
        action = "updated",
        eventType = CPR_PROBATION_ADDRESS_UPDATED,
      )
    }
  }

  private fun publishProbationAddressDomainEvent(
    addressEntity: AddressEntity,
    eventSource: String,
    action: String,
    eventType: String,
  ) {
    val crn = addressEntity.person!!.extractSourceSystemId()!!
    val addressId = addressEntity.updateId
    val detailUrl = "$baseUrl/person/probation/$crn/address/$addressId"

    domainEventPublisher.publish(
      DomainEvent(
        eventType = eventType,
        description = "A probation address has been $action for a person",
        detailUrl = detailUrl,
        occurredAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UK_ZONE).format(Instant.now()),
        additionalInformation = AdditionalInformation(
          outboundCprAddressId = addressId.toString(),
          outboundDeliusAddressId = addressEntity.deliusAddressId,
        ),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("CRN", crn),
          ),
        ),
      ),
      attributes = mapOf("eventSource" to eventSource),
    )
  }
}
