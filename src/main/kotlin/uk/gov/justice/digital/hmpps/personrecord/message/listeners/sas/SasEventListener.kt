package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrived
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_EVENT_QUEUE_ID
import java.util.UUID

@Component
class SasEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val sasClient: SasClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
  private val sasAddressArrivedHandler: SasAddressArrivedHandler,
) {

  @SqsListener(SAS_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is SasAddressUpdated -> processSasAddressUpdated(event)
      is SasAddressDeleted -> processSasAddressDeleted(event)
      is SasAddressArrived -> processSasAddressArrived(event)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun processSasAddressUpdated(event: SasAddressUpdated) {
    val sasResponse = sasClient.getAddress(event.detailUrl)
    addressService.processAddress(
      address = sasResponse.address,
      findPerson = { personRepository.findByCrn(sasResponse.crn)!! },
      findAddress = { personRepository.findByCrn(sasResponse.crn)!!.addresses.first { address -> address.updateId == sasResponse.cprAddressId } },
      eventSource = CPR,
    )
  }

  private fun processSasAddressDeleted(event: SasAddressDeleted) {
    addressService.deleteAddress(
      eventSource = CPR,
      findAddress = { addressRepository.findByUpdateId(UUID.fromString(event.additionalInformation.cprAddressId)) },
    )
  }

  private fun processSasAddressArrived(event: SasAddressArrived) {
    val newMainAddress = sasClient.getAddress(event.detailUrl)
    sasAddressArrivedHandler.setMainAddressToPrevious(newMainAddress.cprAddressId, newMainAddress.address.startDate!!)
    sasAddressArrivedHandler.setProposedAddressToMain(newMainAddress)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
