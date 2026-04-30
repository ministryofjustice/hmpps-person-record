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
import uk.gov.justice.digital.hmpps.personrecord.extensions.nowUtcFormattedUk
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED

@Component
@Profile("!preprod & !prod")
class AddressCreatedListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value("\${core-person-record.base-url}") private val baseUrl: String,
) {

  @EventListener
  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    val addressEntity = addressCreated.addressEntity
    val domainEvent = DomainEvent(
      eventType = CPR_PROBATION_ADDRESS_CREATED,
      personReference = PersonReference(listOf(PersonIdentifier(type = "CRN", value = addressCreated.crn))),
      additionalInformation = AdditionalInformation(cprAddressId = addressEntity.updateId.toString(), deliusAddressId = addressCreated.externalAddressId),
      detailUrl = "$baseUrl/person/probation/${addressCreated.crn}/address/${addressEntity.updateId}",
      description = "Address was created in Core Person Record",
      occurredAt = nowUtcFormattedUk(),
    )
    domainEventPublisher.publish(domainEvent)
  }
}
