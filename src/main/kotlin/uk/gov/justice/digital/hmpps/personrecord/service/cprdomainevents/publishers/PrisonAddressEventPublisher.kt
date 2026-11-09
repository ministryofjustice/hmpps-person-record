package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PRISON_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class PrisonAddressEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : AddressEventPublisher {
  override val sourceSystemType = SourceSystemType.NOMIS

  override fun onCreate(addressCreated: AddressCreated) = with(addressCreated.addressEntity) {
    val prisonNumber = person!!.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprAddressCreated(
        eventType = CPR_PRISON_ADDRESS_CREATED,
        description = "A prison address has been created for a person",
        detailUrl = "$baseUrl/person/prison/$prisonNumber/addresses/$updateId",
        additionalInformation = CprAddressCreatedInfo(cprAddressId = updateId!!),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("prisonNumber", prisonNumber))),
      ),
      attributes = mapOf("eventSource" to addressCreated.eventSource.identifier),
    )
  }

  // Prison address updated and deleted domain events are not published yet
  override fun onUpdate(addressUpdated: AddressUpdated) = Unit

  override fun onDelete(addressDeleted: AddressDeleted) = Unit
}
