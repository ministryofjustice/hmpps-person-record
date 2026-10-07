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
  fun setMainAddressToPrevious(personEntity: PersonEntity, startDate: ZonedDateTime, incomingAddressId: UUID? = null) {
    personEntity.currentMainAddress(incomingAddressId)?.let { oldMainAddress ->
      oldMainAddress.statusCode = P
      oldMainAddress.endDate = startDate
      addressService.processAddress(
        address = Address.from(oldMainAddress),
        findPerson = { personEntity },
        findAddress = { oldMainAddress },
        eventSource = CPR,
      )
    }
  }

  private fun PersonEntity.currentMainAddress(incomingAddressId: UUID?) = addresses.firstOrNull {
    it.statusCode == M && it.updateId != incomingAddressId
  }
}
