package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressDeletedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED

@Component
class ProbationAddressEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : AddressEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onCreate(addressCreated: AddressCreated) = with(addressCreated.addressEntity) {
    val crn = person!!.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprAddressCreated(
        eventType = CPR_PROBATION_ADDRESS_CREATED,
        description = "A probation address has been created for a person",
        detailUrl = "$baseUrl/person/probation/$crn/address/$updateId",
        additionalInformation = CprAddressCreatedInfo(
          cprAddressId = updateId!!,
          deliusAddressId = deliusAddressId.takeIf { addressCreated.eventSource == CPR },
        ),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("CRN", crn))),
      ),
      attributes = mapOf("eventSource" to addressCreated.eventSource.identifier),
    )
  }

  override fun onUpdate(addressUpdated: AddressUpdated) = with(addressUpdated.addressEntity) {
    val crn = person!!.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprAddressUpdated(
        eventType = CPR_PROBATION_ADDRESS_UPDATED,
        description = "A probation address has been updated for a person",
        detailUrl = "$baseUrl/person/probation/$crn/address/$updateId",
        additionalInformation = CprAddressUpdatedInfo(
          cprAddressId = updateId!!,
          deliusAddressId = deliusAddressId.takeIf { addressUpdated.eventSource == CPR },
        ),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("CRN", crn))),
      ),
      attributes = mapOf("eventSource" to addressUpdated.eventSource.identifier),
    )
  }

  override fun onDelete(addressDeleted: AddressDeleted) = with(addressDeleted.addressEntity) {
    val crn = addressDeleted.personEntity.extractSourceSystemId()!!
    domainEventPublisher.publish(
      CprAddressDeleted(
        eventType = CPR_PROBATION_ADDRESS_DELETED,
        description = "A probation address has been deleted for a person",
        additionalInformation = CprAddressDeletedInfo(
          cprAddressId = updateId!!,
          deliusAddressId = deliusAddressId.takeIf { addressDeleted.eventSource == CPR },
        ),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("CRN", crn))),
      ),
      attributes = mapOf("eventSource" to addressDeleted.eventSource.identifier),
    )
  }
}
