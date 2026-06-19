package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.UK_ZONE
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import java.time.Instant
import java.time.format.DateTimeFormatter

interface AddressEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(addressCreated: AddressCreated)
  fun onUpdate(addressUpdated: AddressUpdated)
}

@Component
class ProbationAddressEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : AddressEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onCreate(addressCreated: AddressCreated) {
    publishProbationAddressDomainEvent(
      addressEntity = addressCreated.addressEntity,
      eventSource = addressCreated.eventSource.identifier,
      action = "created",
      eventType = CPR_PROBATION_ADDRESS_CREATED,
    )
  }

  override fun onUpdate(addressUpdated: AddressUpdated) {
    publishProbationAddressDomainEvent(
      addressEntity = addressUpdated.addressEntity,
      eventSource = addressUpdated.eventSource.identifier,
      action = "updated",
      eventType = CPR_PROBATION_ADDRESS_UPDATED,
    )
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
