package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_EVENT_QUEUE_ID
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_ARRIVED
import uk.gov.justice.digital.hmpps.personrecord.service.type.SAS_ADDRESS_UPDATED
import java.time.LocalDate
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
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) { event ->
    when (event.eventType) {
      SAS_ADDRESS_UPDATED -> {
        val sasAddress = sasClient.getAddress(event.detailUrl!!)!!.data
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
      SAS_ADDRESS_ARRIVED -> {
        val sasAddress = sasClient.getAddress(event.detailUrl!!)!!.data
        val personEntity = personRepository.findByCrn(sasAddress.crn)!!

        val demotingAddressEntity = personEntity.addresses.firstOrNull { it.statusCode == AddressStatusCode.M }
        demotingAddressEntity?.let { mainAddressEntity ->
          mainAddressEntity.statusCode = AddressStatusCode.P
          mainAddressEntity.endDate = LocalDate.now().toUkZonedDateTime()
          addressService.processAddress(
            address = Address.from(mainAddressEntity),
            findPerson = { personEntity },
            findAddress = { mainAddressEntity },
            eventSource = DomainEventSource.CPR,
          )
        }

        addressService.processAddress(
          address = Address.from(sasAddress), // <- do we trust typeVerified and addressStatus will be set correctly from sas?
          findPerson = { personEntity },
          findAddress = { personEntity.addresses.first { it.updateId.toString() == sasAddress.cprAddressId } },
          eventSource = DomainEventSource.CPR,
        )
      }
    }
  }
}
