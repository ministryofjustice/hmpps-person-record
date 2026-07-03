package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.SasAddress
import uk.gov.justice.digital.hmpps.personrecord.client.SasClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.SasAddressArrived
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.P
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
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
    val newMainAddress = sasClient.getAddress(event.detailUrl)
    val addressEntity = addressRepository.findByUpdateId(UUID.fromString(event.additionalInformation.cprAddressId))!!
    val personEntity = addressEntity.person!!

    newMainAddress.address.isVerified = true
    newMainAddress.address.statusCode = M
    personEntity.setMainAddressToPrevious(newMainAddress)
    addressService.processAddress(
      address = newMainAddress.address,
      findPerson = { personEntity },
      findAddress = { addressEntity },
      eventSource = CPR,
    )
  }

  private fun PersonEntity.setMainAddressToPrevious(newMainAddress: SasAddress) {
    this.addresses
      .filter { it.updateId != newMainAddress.cprAddressId }
      .firstOrNull { it.statusCode == M }
      ?.let { oldMainAddress ->
        oldMainAddress.statusCode = P
        oldMainAddress.endDate = newMainAddress.address.startDate
        addressService.processAddress(
          address = Address.from(oldMainAddress),
          findPerson = { this },
          findAddress = { oldMainAddress },
          eventSource = CPR,
        )
      }
  }
}
