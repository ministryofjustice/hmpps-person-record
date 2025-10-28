package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.extensions.getPrimary
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity): List<AddressEntity> {
    val newPrimaryAddress = person.addresses.firstOrNull()
    val existingAddress = newPrimaryAddress.findIn(personEntity.addresses)
    return when {
      existingAddress != null -> personEntity.handleExistingAddress(existingAddress)
      else -> personEntity.setNewToPrimaryAddress(newPrimaryAddress)
    }
  }

  private fun PersonEntity.handleExistingAddress(address: AddressEntity): List<AddressEntity> = when {
    address.isPrevious() -> this.setPreviousToPrimary(address)
    else -> this.addresses.map { it }
  }

  private fun PersonEntity.setNewToPrimaryAddress(address: Address?): List<AddressEntity> {
    val addresses = this.addresses.map { it.apply { this.setToPrevious() } }.toMutableList()
    address?.let { addresses.add(AddressEntity.toPrimary(address)) }
    return addresses
  }

  private fun PersonEntity.setPreviousToPrimary(existingAddressEntity: AddressEntity): List<AddressEntity> {
    this.addresses.getPrimary().firstOrNull()?.setToPrevious()
    this.addresses.get(existingAddressEntity)?.setToPrimary()
    return this.addresses.map { it }
  }

  private fun List<AddressEntity>.get(addressEntity: AddressEntity): AddressEntity? = this.find { it.id == addressEntity.id }

  private fun Address?.findIn(addresses: List<AddressEntity>) = addresses.find { address -> Address.from(address) == this }
}
