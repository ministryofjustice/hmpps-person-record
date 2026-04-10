package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.PROBATION_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CREATED

@Component
class ProbationEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val eventProcessor: ProbationEventProcessor,
  private val corePersonRecordAndDeliusClient: CorePersonRecordAndDeliusClient,
  private val addressRepository: AddressRepository,
  private val personRepository: PersonRepository,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED -> {
        val probationAddress = corePersonRecordAndDeliusClient.getAddress(event.additionalInformation?.addressId)
        val person = personRepository.findByCrn(crn)!!

        val addressEntity = probationAddress?.let {
          val coreAddress = Address.from(it)
          val addressEntity = coreAddress?.let { AddressEntity.from(coreAddress) }
          addressEntity!!.person = person
          addressEntity.usages.forEach { usage -> usage.address = addressEntity }
          addressEntity.contacts.forEach { contactEntity -> contactEntity.address = addressEntity }
          addressEntity
        }!!

        addressRepository.save(addressEntity)
      }
      else -> {
        corePersonRecordAndDeliusClient.getPerson(crn).let {
          eventProcessor.processEvent(it)
        }
      }
    }
  }
}
