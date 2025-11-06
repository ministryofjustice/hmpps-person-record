package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity?): List<Address> {
    val primaryAddress = person.addresses.firstOrNull()?.setToPrimary()
    val previousAddresses = setToPrevious(personEntity?.addresses, primaryAddress)
    return listOf(*previousAddresses, primaryAddress).mapNotNull { it }
  }

  fun setToPrevious(addresses: List<AddressEntity>?, newAddress: Address?): Array<Address> = addresses?.map { Address.from(it) }
    ?.filterNot { compareAddressTo(newAddress, it) }
    ?.map {
      it.setToPrevious()
    }?.toTypedArray() ?: arrayOf()

  private fun compareAddressTo(oneAddress: Address?, anotherAddress: Address): Boolean = anotherAddress.copy(recordType = null) == oneAddress?.copy(recordType = null)
}
