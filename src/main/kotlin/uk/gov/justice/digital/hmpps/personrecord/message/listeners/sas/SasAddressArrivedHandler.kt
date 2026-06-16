package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.time.LocalDate
import java.util.UUID

@Component
class SasAddressArrivedHandler(
  private val sasClient: SasClient,
  private val personRepository: PersonRepository,
  private val addressService: AddressService,
) {

  fun handle(event: DomainEvent) {
    val sasAddress = sasClient.getAddress(event.detailUrl!!)!!.data
    val personEntity = personRepository.findByCrn(sasAddress.crn)!!
    val cprAddressId = sasAddress.cprAddressId
    personEntity.demoteMainAddressIfPresent(cprAddressId)

    addressService.processAddress(
      address = Address.from(sasAddress),
      findPerson = { personEntity },
      findAddress = { personEntity.addresses.first { it.updateId.toString() == cprAddressId } },
      eventSource = DomainEventSource.CPR,
    )
  }

  private fun PersonEntity.demoteMainAddressIfPresent(cprAddressIdToIgnore: String) {
    this.addresses
      .filter { it.updateId != UUID.fromString(cprAddressIdToIgnore) }
      .firstOrNull { it.statusCode == AddressStatusCode.M }
      ?.let { mainAddressEntity ->
        mainAddressEntity.statusCode = AddressStatusCode.P
        mainAddressEntity.endDate = LocalDate.now().toUkZonedDateTime()
        addressService.processAddress(
          address = Address.from(mainAddressEntity),
          findPerson = { this },
          findAddress = { mainAddressEntity },
          eventSource = DomainEventSource.CPR,
        )
      }
  }
}
