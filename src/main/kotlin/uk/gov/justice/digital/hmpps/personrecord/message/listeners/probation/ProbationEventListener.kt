package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SqsDomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED

@Component
class ProbationEventListener(
  private val sqsDomainEventProcessor: SqsDomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
  private val deliusAddressIdHandler: DeliusAddressIdHandler,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsDomainEventProcessor.processDomainEvent(rawMessage) { event, eventSource ->
    when {
      patchAddressEvent(event, eventSource) -> deliusAddressIdHandler.patchAddress(event)
      createAddressEvent(event, eventSource) -> upsertAddress(event)
      updateAddressEvent(event, eventSource) -> upsertAddress(event)
      ignoreUpdateAddressEvent(event, eventSource) -> {}
      deleteAddressEvent(event) -> deleteAddress(event)
      else -> updateWholePerson(event)
    }
  }

  private fun patchAddressEvent(event: DomainEvent, eventSource: String?) = eventSource == DomainEventSource.CPR.identifier && event.eventType == OFFENDER_ADDRESS_CREATED

  private fun createAddressEvent(event: DomainEvent, eventSource: String?) = eventSource != DomainEventSource.CPR.identifier && event.eventType == OFFENDER_ADDRESS_CREATED

  private fun updateAddressEvent(event: DomainEvent, eventSource: String?) = eventSource != DomainEventSource.CPR.identifier && event.eventType == OFFENDER_ADDRESS_UPDATED

  private fun ignoreUpdateAddressEvent(event: DomainEvent, eventSource: String?) = eventSource == DomainEventSource.CPR.identifier && event.eventType == OFFENDER_ADDRESS_UPDATED

  private fun deleteAddressEvent(event: DomainEvent) = event.eventType == OFFENDER_ADDRESS_DELETED

  private fun upsertAddress(event: DomainEvent) {
    val crn = event.getCrn()
    val probationAddress = getProbationAddress(event)
    val personEntity = personRepository.findByCrn(crn)!!

    addressService.processAddress(
      address = Address.from(probationAddress)!!,
      findPerson = { personEntity },
      findAddress = { personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } },
      eventSource = DELIUS,
    )
  }

  private fun deleteAddress(event: DomainEvent) {
    event.additionalInformation?.inboundDeliusAddressId?.let {
      val deliusAddressId = it.toLong()
      addressService.deleteAddress { addressRepository.findByDeliusAddressId(deliusAddressId) }
    }
  }

  private fun updateWholePerson(event: DomainEvent) {
    corePersonRecordAndDeliusClient.getPerson(event.getCrn()).let {
      eventProcessor.processEvent(it, setOf(AddressEntity::class))
    }
  }

  private fun getProbationAddress(event: DomainEvent): ProbationAddress {
    val deliusAddressId = event.additionalInformation?.inboundDeliusAddressId!!
    return corePersonRecordAndDeliusClient.getAddress(deliusAddressId)!!
  }
}
