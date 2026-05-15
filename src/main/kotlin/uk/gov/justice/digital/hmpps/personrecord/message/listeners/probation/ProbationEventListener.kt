package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ConflictException
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
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
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
  private val reclusterService: ReclusterService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED -> {
        val probationAddress = getProbationAddress(event)

        // TODO: Once ready, make use of the new AddressService class
        val personEntity = personRepository.findByCrn(crn)!!
        if (personEntity.addresses.firstOrNull { it.deliusAddressId == probationAddress.deliusAddressId } != null) {
          throw ConflictException("Probation address with deliusAddressId '${probationAddress.deliusAddressId}' already exists")
        }

        val addressEntity = AddressEntity.from(Address.from(probationAddress)!!)
        addressEntity.person = personEntity
        addressEntity.usages.forEach { usage -> usage.address = addressEntity }
        addressEntity.contacts.forEach { contactEntity -> contactEntity.address = addressEntity }

        addressRepository.save(addressEntity)
        // TODO if...
        reclusterService.recluster(personEntity)
      }
      OFFENDER_ADDRESS_UPDATED -> {
        val probationAddress = getProbationAddress(event)

        val personEntity = personRepository.findByCrn(crn)!!
        val addressEntity = personEntity.addresses.first { it.deliusAddressId == probationAddress.deliusAddressId }
        addressEntity.update(Address.from(probationAddress)!!)

        addressRepository.save(addressEntity)
        // TODO if...
        reclusterService.recluster(personEntity)
      }
      OFFENDER_ADDRESS_DELETED -> {
        val probationAddress = getProbationAddress(event)

        val personEntity = personRepository.findByCrn(crn)!!
        val addressEntity = personEntity.addresses.first { it.deliusAddressId == probationAddress.deliusAddressId }

        personEntity.addresses.remove(addressEntity)
        addressEntity.person = null
        personRepository.save(personEntity)
        // TODO if...
        reclusterService.recluster(personEntity)
      }
      else -> {
        corePersonRecordAndDeliusClient.getPerson(crn).let {
          eventProcessor.processEvent(it)
        }
      }
    }
  }

  private fun getProbationAddress(event: DomainEvent): ProbationAddress {
    val deliusAddressCallbackUrl = event.detailUrl!!
    return corePersonRecordAndDeliusClient.getAddress(deliusAddressCallbackUrl)!!
  }
}
