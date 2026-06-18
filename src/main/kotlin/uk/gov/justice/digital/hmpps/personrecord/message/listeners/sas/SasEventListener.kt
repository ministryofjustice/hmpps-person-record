package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_EVENT_QUEUE_ID

@Component
@Profile("!preprod | !prod")
class SasEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val sasClient: SasClient,
  private val personRepository: PersonRepository,
  private val addressService: AddressService,
) {

  @SqsListener(SAS_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process(rawMessage) { event ->
    when (event) {
      is SasAddressUpdated -> processSasAddressUpdated(event)
      else -> {} // no-op, we only expect address updates from SAS for now
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
}
