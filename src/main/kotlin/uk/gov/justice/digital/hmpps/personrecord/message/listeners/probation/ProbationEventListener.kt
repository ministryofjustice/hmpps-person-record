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
      is ProbationAddressCreated -> processOffenderAddressCreated(event)
      is ProbationAddressUpdated -> processOffenderAddressUpdated(event)
      is ProbationAddressDeleted -> processOffenderAddressDeleted(event)
      is ProbationPersonCreated -> processOffenderCreated(event)
      is ProbationPersonUpdated -> processOffenderUpdated(event)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  private fun processOffenderCreated(event: ProbationPersonCreated) {
    updateWholePerson(event.crn)
  }

  private fun processOffenderUpdated(event: ProbationPersonUpdated) {
    updateWholePerson(event.crn)
  }

  private fun processOffenderAddressCreated(event: ProbationAddressCreated) {
    upsertAddress(event.crn, event.additionalInformation.deliusAddressId)
  }

  private fun processOffenderAddressUpdated(event: ProbationAddressUpdated) {
    upsertAddress(event.crn, event.additionalInformation.deliusAddressId)
  }

  private fun processOffenderAddressDeleted(event: ProbationAddressDeleted) {
    addressService.deleteAddress(
      eventSource = DELIUS,
      findAddress = { addressRepository.findByDeliusAddressId(event.additionalInformation.deliusAddressId) },
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
      eventProcessor.processEvent(it, setOf(AddressEntity::class))
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
