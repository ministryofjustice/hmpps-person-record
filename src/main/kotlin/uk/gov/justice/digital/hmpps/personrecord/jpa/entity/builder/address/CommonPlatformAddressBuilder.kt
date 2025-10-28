package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.extensions.getPrimary
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.RecordType

object CommonPlatformAddressBuilder {

  fun build(person: Person, personEntity: PersonEntity): List<AddressEntity> {
    val newPrimaryAddress = person.addresses.first()
    val existingAddress = newPrimaryAddress.findIn(personEntity.addresses)
    return when {
      existingAddress != null -> personEntity.handleExistingAddress(existingAddress)
      else -> personEntity.setNewToPrimaryAddress(newPrimaryAddress)
    }
  }

  private fun PersonEntity.handleExistingAddress(address: AddressEntity): List<AddressEntity> {
    return when {
      address.isPrevious() -> this.setPreviousToPrimary(address)
      else -> this.addresses.map { it }
    }
  }

  private fun PersonEntity.setNewToPrimaryAddress(address: Address): List<AddressEntity> {
    val addresses = this.addresses.map { it.apply { recordType = RecordType.PREVIOUS } }.toMutableList()
    addresses.add(AddressEntity.toPrimary(address))
    return addresses
  }

  private fun PersonEntity.setPreviousToPrimary(existingAddressEntity: AddressEntity): List<AddressEntity> {
    this.addresses.getPrimary().firstOrNull()?.let { it.recordType = RecordType.PREVIOUS }
    this.addresses.find { it.id == existingAddressEntity.id }?.let { it.recordType = RecordType.PRIMARY }
    return this.addresses.map { it }
  }

  private fun Address?.findIn(addresses: List<AddressEntity>) = addresses.find { address -> Address.from(address) == this }
}