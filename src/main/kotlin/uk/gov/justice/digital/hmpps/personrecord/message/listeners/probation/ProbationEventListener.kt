package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.queue.SQSListenerService
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED
import java.util.UUID

@Component
class ProbationEventListener(
  private val sqsListenerService: SQSListenerService,
  private val jsonMapper: JsonMapper,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsListenerService.processSQSMessage(rawMessage) { message ->
    val event = jsonMapper.readValue<DomainEvent>(message.message)
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED, OFFENDER_ADDRESS_UPDATED -> {
        val probationAddress = getProbationAddress(event)
        val personEntity = personRepository.findByCrn(crn)!!

        addressService.processAddress(
          address = Address.from(probationAddress)!!,
          findPerson = { personEntity },
          findAddress = { existingAddress(personEntity, probationAddress, event) },
          eventSource = getProbationEventSource(message),
        )
      }
      OFFENDER_ADDRESS_DELETED -> {
        event.additionalInformation?.inboundDeliusAddressId?.let {
          val deliusAddressId = it.toLong()
          addressService.deleteAddress { addressRepository.findByDeliusAddressId(deliusAddressId) }
        }
      }
      else -> {
        corePersonRecordAndDeliusClient.getPerson(crn).let {
          eventProcessor.processEvent(it, setOf(AddressEntity::class))
        }
      }
    }
  }

  private fun existingAddress(
    personEntity: PersonEntity,
    probationAddress: ProbationAddress,
    event: DomainEvent,
  ): AddressEntity? = personEntity.addresses.firstOrNull {
    it.deliusAddressId == probationAddress.deliusAddressId ||
      it.updateId == event.additionalInformation?.cprAddressId?.let { cprAddressId ->
        UUID.fromString(cprAddressId)
      }
  }

  private fun getProbationEventSource(message: SQSMessage): DomainEventSource = message.getEventSource()?.let { src -> DomainEventSource.entries.associateBy { it.identifier }[src] } ?: DELIUS

  private fun getProbationAddress(event: DomainEvent): ProbationAddress {
    val deliusAddressId = event.additionalInformation?.inboundDeliusAddressId!!
    return corePersonRecordAndDeliusClient.getAddress(deliusAddressId)!!
  }
}
