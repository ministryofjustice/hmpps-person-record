package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID

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
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processHmppsDomainEvent<HmppsDomainEvent>(rawMessage) { event ->
    when (event) {
      is ProbationAddressCreated -> upsertAddress(event.crn, event.additionalInformation.deliusAddressId)
      is ProbationAddressUpdated -> upsertAddress(event.crn, event.additionalInformation.deliusAddressId)
      is ProbationAddressDeleted -> deleteAddress(event.additionalInformation.deliusAddressId)
      is ProbationPersonCreated -> updateWholePerson(event.crn)
      is ProbationPersonUpdated -> updateWholePerson(event.crn)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun deleteAddress(deliusAddressId: Long) {
    addressService.deleteAddress(
      eventSource = DELIUS,
      findAddress = { addressRepository.findByDeliusAddressId(deliusAddressId) },
    )
  }

  private fun upsertAddress(crn: String, deliusAddressId: Long) {
    val probationAddress = corePersonRecordAndDeliusClient.getAddress(deliusAddressId)!!
    addressService.processAddress(
      address = probationAddress,
      findPerson = { personRepository.findByCrn(crn)!! },
      findAddress = { addressRepository.findByDeliusAddressId(deliusAddressId) },
      eventSource = DELIUS,
    )
  }

  private fun updateWholePerson(crn: String) {
    corePersonRecordAndDeliusClient.getPerson(crn).let {
      eventProcessor.processEvent(it)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
