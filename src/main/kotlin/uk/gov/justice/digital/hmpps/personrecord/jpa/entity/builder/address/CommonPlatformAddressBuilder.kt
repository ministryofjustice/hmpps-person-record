package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity?): List<Address?> {
    val newPrimaryAddress = person.addresses.firstOrNull()
    newPrimaryAddress?.setToPrimary()
    val existingAddress = newPrimaryAddress.findInEntities(personEntity?.addresses)
    return when {
      existingAddress != null -> rebuildAddresses(existingAddress, personEntity?.addresses)
      else -> setNewToPrimaryAddress(newPrimaryAddress, personEntity?.addresses)
    }
  }

  private fun rebuildAddresses(address: Address, existingAddresses: MutableList<AddressEntity>?): List<Address?> {
    address.setToPrimary()
    val newAddresses = mutableListOf<Address?>(address)
    val otherAddresses: List<Address>? = address.extract(existingAddresses)
    otherAddresses?.forEach { it.setToPrevious() }
    otherAddresses?.let { newAddresses.addAll(it) }
    return newAddresses
  }

  private fun setNewToPrimaryAddress(address: Address?, existingAddresses: MutableList<AddressEntity>?): List<Address?> {
    val newAddresses = mutableListOf(address)

    existingAddresses?.forEach {
      val a = Address.from(it)
      a.setToPrevious()
      newAddresses.add(a)
    }

    return newAddresses.toList()
  }

  private fun Address?.extract(addresses: List<AddressEntity>?) = addresses?.filter { address -> this?.compareAddressTo(Address.from(address)) == false }?.map { Address.from(it) }
  private fun Address?.findInEntities(addresses: List<AddressEntity>?) = addresses?.find { address -> this?.compareAddressTo(Address.from(address)) == true }?.let { Address.from(it) }
}
