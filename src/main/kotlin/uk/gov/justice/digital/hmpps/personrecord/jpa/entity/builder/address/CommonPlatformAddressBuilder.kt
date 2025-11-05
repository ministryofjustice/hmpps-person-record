package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.RecordType

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity): List<Address> {
    val newPrimaryAddress = person.addresses.firstOrNull()
    newPrimaryAddress?.setToPrimary()
    val existingAddress = newPrimaryAddress.findInEntities(personEntity.addresses)
    return when {
      existingAddress != null -> person.handleExistingAddress(existingAddress)
      else -> person.setNewToPrimaryAddress(newPrimaryAddress)
    }
  }

  private fun Person.handleExistingAddress(address: Address): List<Address> = when {
    address.isPrevious() -> this.setPreviousToPrimary(address)
    else -> this.addresses.map { it }
  }

  private fun Person.setNewToPrimaryAddress(address: Address?): List<Address> {
    val addresses = this.addresses.map { it.apply { this.setToPrevious() } }.toMutableList()
    address?.let { addresses.add(address) }
    return addresses
  }

  private fun Person.setPreviousToPrimary(existingAddressEntity: Address): List<Address> {
    this.addresses.getPrimary().firstOrNull()?.setToPrevious()
    existingAddressEntity.findIn(this.addresses)?.setToPrimary()
    return this.addresses.map { it }
  }

  private fun Address?.findInEntities(addresses: List<AddressEntity>) = addresses.find { address -> Address.from(address) == this }?.let { Address.from(it) }
  private fun Address?.findIn(addresses: List<Address>) = addresses.find { address -> address == this }

  fun List<Address>.getPrimary(): List<Address> = this.getByType(RecordType.PRIMARY)
  fun List<Address>.getPrevious(): List<Address> = this.getByType(RecordType.PREVIOUS)
  private fun List<Address>.getByType(type: RecordType): List<Address> = this.filter { it.recordType == type }
}
