package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity?): List<Address> {
    val primaryAddress = person.addresses.firstOrNull()
    return rebuildAddresses(primaryAddress, personEntity?.addresses)
  }

  private fun rebuildAddresses(address: Address?, existingAddresses: MutableList<AddressEntity>?): List<Address> {
    address?.setToPrimary()
    val newAddresses = mutableListOf(address)
    val otherAddresses: List<Address>? = extract(existingAddresses, address)
    otherAddresses?.forEach { it.setToPrevious() }
    otherAddresses?.let { newAddresses.addAll(it) }
    return newAddresses.mapNotNull { it }
  }

  fun extract(addresses: List<AddressEntity>?, newAddress: Address?): List<Address>? {
    if (newAddress == null) return addresses?.map { Address.from(it) }
    return addresses?.filter { address ->
      newAddress.compareAddressTo(Address.from(address)) == false
    }?.map { Address.from(it) }
  }

  private fun Address?.compareAddressTo(anotherAddress: Address): Boolean = this?.copy(recordType = null) == anotherAddress.copy(recordType = null)
}
