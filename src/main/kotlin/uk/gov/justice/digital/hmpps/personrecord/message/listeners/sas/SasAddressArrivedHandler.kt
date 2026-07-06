package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.SasAddress
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.AddressRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.M
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode.P
import uk.gov.justice.digital.hmpps.personrecord.service.DomainEventSource.CPR
import uk.gov.justice.digital.hmpps.personrecord.service.address.AddressService
import java.time.ZonedDateTime
import java.util.UUID

@Component
class SasAddressArrivedHandler(
  private val addressRepository: AddressRepository,
  private val addressService: AddressService,
) {

  @Transactional
  fun setProposedAddressToMain(newMainAddress: SasAddress) {
    newMainAddress.address.isVerified = true
    newMainAddress.address.statusCode = M

    addressService.processAddress(
      address = newMainAddress.address,
      findAddress = { addressRepository.findByUpdateId(newMainAddress.cprAddressId)!! },
      eventSource = CPR,
    )
  }

  @Transactional
  fun setMainAddressToPrevious(cprAddressId: UUID, startDate: ZonedDateTime) {
    val addressEntity = addressRepository.findByUpdateId(cprAddressId)!!
    val personEntity = addressEntity.person!!
    personEntity.setMainAddressToPrevious(cprAddressId, startDate)
  }

  fun PersonEntity.setMainAddressToPrevious(cprAddressId: UUID, startDate: ZonedDateTime) {
    this.addresses
      .filter { it.updateId != cprAddressId }
      .firstOrNull { it.statusCode == M }
      ?.let { oldMainAddress ->
        oldMainAddress.statusCode = P
        oldMainAddress.endDate = startDate
        addressService.processAddress(
          address = Address.from(oldMainAddress),
          findPerson = { this },
          findAddress = { oldMainAddress },
          eventSource = CPR,
        )
      }
  }
}
