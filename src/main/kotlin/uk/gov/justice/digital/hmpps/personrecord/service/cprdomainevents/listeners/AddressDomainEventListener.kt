package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprAddressUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.extensions.asStringWithUkZone
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.address.AddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_ADDRESS_UPDATED
import java.time.Instant

@Profile("!preprod & !prod")
@Component
class AddressDomainEventListener(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  @TransactionalEventListener
  fun onAddressCreated(addressCreated: AddressCreated) {
    val config = buildAddressDomainEventConfig(addressCreated.addressEntity.person!!.sourceSystem) ?: return
    val sourceSystemId = addressCreated.addressEntity.person!!.extractSourceSystemId()!!
    val addressId = addressCreated.addressEntity.updateId

    domainEventPublisher.publish(
      CprAddressCreated(
        eventType = CPR_PROBATION_ADDRESS_CREATED,
        description = "A ${config.typeDescription} address has been created for a person",
        detailUrl = "$baseUrl/person/${config.urlPathSegment}/$sourceSystemId/address/$addressId",
        occurredAt = Instant.now().asStringWithUkZone(),
        additionalInformation = CprAddressCreatedInfo(
          cprAddressId = addressId.toString(),
          deliusAddressId = addressCreated.addressEntity.deliusAddressId,
        ),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(config.identifierName, sourceSystemId),
          ),
        ),
      ),
      attributes = mapOf("eventSource" to addressCreated.eventSource.identifier),
    )
  }

  @TransactionalEventListener
  fun onAddressUpdated(addressUpdated: AddressUpdated) {
    val config = buildAddressDomainEventConfig(addressUpdated.addressEntity.person!!.sourceSystem) ?: return
    val sourceSystemId = addressUpdated.addressEntity.person!!.extractSourceSystemId()!!
    val addressId = addressUpdated.addressEntity.updateId

    domainEventPublisher.publish(
      CprAddressUpdated(
        eventType = CPR_PROBATION_ADDRESS_UPDATED,
        description = "A ${config.typeDescription} address has been updated for a person",
        detailUrl = "$baseUrl/person/${config.urlPathSegment}/$sourceSystemId/address/$addressId",
        occurredAt = Instant.now().asStringWithUkZone(),
        additionalInformation = CprAddressUpdatedInfo(
          cprAddressId = addressId.toString(),
          deliusAddressId = addressUpdated.addressEntity.deliusAddressId,
        ),
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier(config.identifierName, sourceSystemId),
          ),
        ),
      ),
      attributes = mapOf("eventSource" to addressUpdated.eventSource.identifier),
    )
  }

  private fun buildAddressDomainEventConfig(sourceSystem: SourceSystemType): AddressDomainEventConfig? = when (sourceSystem) {
    DELIUS -> AddressDomainEventConfig("CRN", "probation", "probation")
    else -> null // temporary flow for other source systems we haven't mapped yet
  }
}

data class AddressDomainEventConfig(
  val identifierName: String,
  val urlPathSegment: String,
  val typeDescription: String,
)
