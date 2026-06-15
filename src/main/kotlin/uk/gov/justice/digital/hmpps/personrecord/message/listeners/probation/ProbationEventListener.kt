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
