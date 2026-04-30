package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_ADDRESS_UPDATED
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
@Profile("!preprod & !prod")
class AddressEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value("\${core-person-record.base-url}") private val baseUrl: String,
) {

  @EventListener
  @TransactionalEventListener
  fun onAddressUpdate(addressUpdated: AddressUpdated) {
    val addressEntity = addressUpdated.addressEntity
    val domainEvent = DomainEvent(
      eventType = CPR_ADDRESS_UPDATED,
      personReference = PersonReference(listOf(PersonIdentifier(type = "CRN", value = addressUpdated.crn))),
      additionalInformation = AdditionalInformation(cprAddressId = addressEntity.updateId.toString()),
      detailUrl = "$baseUrl/person/probation/${addressUpdated.crn}/address/${addressEntity.updateId}",
      description = "Address was updated in Core Person Record",
      occurredAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Europe/London")).format(Instant.now()),
    )
    domainEventPublisher.publish(domainEvent)
  }
}
