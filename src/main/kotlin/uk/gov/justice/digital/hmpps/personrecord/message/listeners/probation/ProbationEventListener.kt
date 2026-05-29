package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
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
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_DELETED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_UPDATED

@Component
@Profile("preprod & prod")
class ProbationEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    logger.info("Saving person including address")
    val crn = event.getCrn()
    corePersonRecordAndDeliusClient.getPerson(crn).let {
      eventProcessor.processEvent(it)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(ProbationEventListener::class.java)
  }
}

@Component
@Profile("!preprod & !prod")
class ProbationEventListenerDev(
  private val domainEventProcessor: DomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val personRepository: PersonRepository,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEventDev(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    logger.info("Saving person and address separately")
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED, OFFENDER_ADDRESS_UPDATED -> {
        val probationAddress = getProbationAddress(event)
        val personEntity = personRepository.findByCrn(crn)!!

        addressService.processAddress(
          address = Address.from(probationAddress)!!,
          findPerson = { personEntity },
          findAddress = { personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } },
        )
      }
      OFFENDER_ADDRESS_DELETED -> {
        event.additionalInformation?.deliusAddressId?.let {
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
    val deliusAddressCallbackUrl = event.detailUrl!!
    return corePersonRecordAndDeliusClient.getAddress(deliusAddressCallbackUrl)!!
  }

  companion object {
    private val logger = LoggerFactory.getLogger(ProbationEventListenerDev::class.java)
  }
}
