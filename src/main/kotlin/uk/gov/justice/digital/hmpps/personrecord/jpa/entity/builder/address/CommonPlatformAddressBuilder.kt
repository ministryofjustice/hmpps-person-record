package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity?): List<Address> {
    val primaryAddress = person.addresses.firstOrNull()
    primaryAddress?.setToPrimary()
    val newAddresses = mutableListOf(primaryAddress)
    val otherAddresses: List<Address>? = removePrimaryAddress(personEntity?.addresses, primaryAddress)
    otherAddresses?.forEach { it.setToPrevious() }
    otherAddresses?.let { newAddresses.addAll(it) }
    return newAddresses.mapNotNull { it }
  }

  fun removePrimaryAddress(addresses: List<AddressEntity>?, newAddress: Address?): List<Address>? = when {
    newAddress == null -> addresses
    else -> addresses?.filter { address ->
      newAddress.compareAddressTo(Address.from(address))
    }
  }?.map { Address.from(it) }

  private fun Address.compareAddressTo(anotherAddress: Address): Boolean = this.copy(recordType = null) != anotherAddress.copy(recordType = null)
}
