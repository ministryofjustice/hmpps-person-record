package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressCreatedUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderAddressDeleted
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.ProbationOffenderCreatedUpdated
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
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process(rawMessage) { event ->
    when (event) {
      is ProbationOffenderAddressCreatedUpdated -> processOffenderAddressCreatedUpdated(event)
      is ProbationOffenderAddressDeleted -> processOffenderAddressDeleted(event)
      else -> processOffenderCreatedUpdated(event as ProbationOffenderCreatedUpdated)
    }
  }

  private fun processOffenderAddressCreatedUpdated(event: ProbationOffenderAddressCreatedUpdated) {
    val probationAddress = corePersonRecordAndDeliusClient.getAddress(event.additionalInformation.deliusAddressId)
    val personEntity = personRepository.findByCrn(event.crn)!!

    addressService.processAddress(
      address = probationAddress!!,
      findPerson = { personEntity },
      findAddress = { personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } },
      eventSource = DELIUS,
    )
  }

  private fun processOffenderAddressDeleted(event: ProbationOffenderAddressDeleted) {
    addressService.deleteAddress { addressRepository.findByDeliusAddressId(event.additionalInformation.deliusAddressId) }
  }

  private fun processOffenderCreatedUpdated(event: ProbationOffenderCreatedUpdated) {
    corePersonRecordAndDeliusClient.getPerson(event.crn).let {
      eventProcessor.processEvent(it, setOf(AddressEntity::class))
    }
  }
}
