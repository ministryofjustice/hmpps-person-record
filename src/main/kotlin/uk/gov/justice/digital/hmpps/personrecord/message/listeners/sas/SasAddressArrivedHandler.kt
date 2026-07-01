package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.SasAddress
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrived
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.util.UUID

@Component
class SasAddressArrivedHandler(
  private val sasClient: SasClient,
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @Transactional
  fun handle(event: SasAddressArrived) {
    val sasAddress = sasClient.getAddress(event.detailUrl)
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(event.additionalInformation.cprAddressId))!!
    val personEntity = addressEntity.person!!

    sasAddress.address.isVerified = true
    sasAddress.address.statusCode = AddressStatusCode.M
    personEntity.demoteMainAddressIfPresent(sasAddress)
    addressService.processAddress(
      address = sasAddress.address,
      findPerson = { personEntity },
      findAddress = { addressEntity },
      eventSource = DomainEventSource.CPR,
    )
  }

  private fun PersonEntity.demoteMainAddressIfPresent(sasAddress: SasAddress) {
    this.addresses
      .filter { it.updateId != sasAddress.cprAddressId }
      .firstOrNull { it.statusCode == AddressStatusCode.M }
      ?.let { mainAddressEntityBeingDemoted ->
        mainAddressEntityBeingDemoted.statusCode = AddressStatusCode.P
        mainAddressEntityBeingDemoted.endDate = sasAddress.address.startDate
        addressService.processAddress(
          address = Address.from(mainAddressEntityBeingDemoted),
          findPerson = { this },
          findAddress = { mainAddressEntityBeingDemoted },
          eventSource = DomainEventSource.CPR,
        )
      }
  }
}
