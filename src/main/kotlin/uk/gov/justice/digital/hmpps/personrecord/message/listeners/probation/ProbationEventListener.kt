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
import java.util.UUID

@Component
class ProbationEventListener(
  private val sqsDomainEventProcessor: SqsDomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = sqsDomainEventProcessor.processDomainEvent(rawMessage) { event, eventSource ->
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED -> {
        when (eventSource != DomainEventSource.CPR.identifier) {
          true -> {
            val probationAddress = getProbationAddress(event)
            val personEntity = personRepository.findByCrn(crn)!!

            addressService.processAddress(
              address = Address.from(probationAddress)!!,
              findPerson = { personEntity },
              findAddress = { personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } },
              eventSource = DELIUS,
            )
          }
          false -> {
            val cprAddressUpdateId = event.additionalInformation!!.cprAddressId!!
            val probationAddress = getProbationAddress(event)
            val existingAddressEntity = addressRepository.findByUpdateId(UUID.fromString(cprAddressUpdateId))!!
            existingAddressEntity.deliusAddressId = probationAddress.deliusAddressId
            addressRepository.save(existingAddressEntity)
          }
        }
      }
      OFFENDER_ADDRESS_UPDATED -> {
        val probationAddress = getProbationAddress(event)
        val personEntity = personRepository.findByCrn(crn)!!

        addressService.processAddress(
          address = Address.from(probationAddress)!!,
          findPerson = { personEntity },
          findAddress = { personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } },
          eventSource = DELIUS,
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

  private fun getProbationAddress(event: DomainEvent): ProbationAddress {
    val deliusAddressId = event.additionalInformation?.inboundDeliusAddressId!!
    return corePersonRecordAndDeliusClient.getAddress(deliusAddressId)!!
  }
}
