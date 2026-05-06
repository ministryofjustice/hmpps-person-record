package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.CorePersonRecordAndDeliusClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getCrn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.AddressCreated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
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
  private val publisher: ApplicationEventPublisher,
  private val reclusterService: ReclusterService,
) {

  @SqsListener(PROBATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    val crn = event.getCrn()
    when (event.eventType) {
      OFFENDER_ADDRESS_CREATED -> {
        val deliusAddressCallbackUrl = event.detailUrl!!
        val deliusAddressId = event.additionalInformation?.addressId!!
        val probationAddress = corePersonRecordAndDeliusClient.getAddress(deliusAddressCallbackUrl)!!

        // TODO: Once ready, make use of the new AddressService class
        val personEntity = personRepository.findByCrn(crn)!!
        val addressEntity = AddressEntity.from(Address.from(probationAddress)!!)
        addressEntity.person = personEntity
        addressEntity.usages.forEach { usage -> usage.address = addressEntity }
        addressEntity.contacts.forEach { contactEntity -> contactEntity.address = addressEntity }

        addressRepository.save(addressEntity)
        publisher.publishEvent(AddressCreated(crn, deliusAddressId, addressEntity))

        reclusterService.recluster(personEntity)
      }
      else -> {
        corePersonRecordAndDeliusClient.getPerson(crn).let {
          eventProcessor.processEvent(it)
        }
      }
    }
  }
}
