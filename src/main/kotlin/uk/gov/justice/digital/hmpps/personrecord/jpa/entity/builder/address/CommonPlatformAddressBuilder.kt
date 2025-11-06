package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity?): List<Address> {
    // if null, return previous addresses
    // if new, set to primary and return previous addresses
    val primaryAddress = person.addresses.firstOrNull()?.setToPrimary()
    val previousAddresses = removePrimaryAddress(personEntity?.addresses, primaryAddress)
    return listOf(*previousAddresses, primaryAddress).mapNotNull { it }
  }

  fun removePrimaryAddress(addresses: List<AddressEntity>?, newAddress: Address?): Array<Address> = when {
    newAddress == null -> addresses
    else -> addresses?.filter { address ->
      newAddress.compareAddressTo(Address.from(address))
    }
  }?.map {
    Address.from(it).setToPrevious()
  }?.toTypedArray() ?: arrayOf()

  private fun Address.compareAddressTo(anotherAddress: Address): Boolean = this.copy(recordType = null) != anotherAddress.copy(recordType = null)
}
