package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_EVENT_QUEUE_ID
import java.util.UUID

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
    val sasAddress = sasClient.getAddress(event.detailUrl)!!.data
    val cprAddressUpdateId = UUID.fromString(sasAddress.cprAddressId)
    val updatedSasAddress = Address.from(sasAddress)
    val personEntity = personRepository.findByCrn(sasAddress.crn)!!

    addressService.processAddress(
      address = updatedSasAddress,
      findPerson = { personEntity },
      findAddress = { personEntity.addresses.first { address -> address.updateId == cprAddressUpdateId } },
      eventSource = DomainEventSource.CPR,
    )
  }
}
