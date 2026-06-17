package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED

@Component
class ProbationEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    when {
      createAddressEvent(event) -> upsertAddress(event)
      updateAddressEvent(event) -> upsertAddress(event)
      deleteAddressEvent(event) -> deleteAddress(event)
      else -> updateWholePerson(event)
    }
  }

  private fun createAddressEvent(event: DomainEvent) = event.eventType == OFFENDER_ADDRESS_CREATED

  private fun updateAddressEvent(event: DomainEvent) = event.eventType == OFFENDER_ADDRESS_UPDATED

  private fun deleteAddressEvent(event: DomainEvent) = event.eventType == OFFENDER_ADDRESS_DELETED

  private fun upsertAddress(event: DomainEvent) {
    val deliusAddressId = event.getDeliusAddressId()!!
    val probationAddress = corePersonRecordAndDeliusClient.getAddress(deliusAddressId)!!
    addressService.processAddress(
      address = probationAddress,
      findPerson = { personRepository.findByCrn(event.getCrn())!! },
      findAddress = { personRepository.findByCrn(event.getCrn())?.addresses?.firstOrNull { it.deliusAddressId == deliusAddressId } },
      eventSource = DELIUS,
    )
  }

  private fun deleteAddress(event: DomainEvent) = addressService.deleteAddress {
    addressRepository.findByDeliusAddressId(event.getDeliusAddressId())
  }

  private fun DomainEvent.getDeliusAddressId(): Long? = this.additionalInformation?.inboundDeliusAddressId

  private fun updateWholePerson(event: DomainEvent) {
    corePersonRecordAndDeliusClient.getPerson(event.getCrn()).let {
      eventProcessor.processEvent(it, setOf(AddressEntity::class))
    }
  }
}
