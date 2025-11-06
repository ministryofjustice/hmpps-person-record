package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

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

  private fun setToPrevious(addresses: List<AddressEntity>?, newAddress: Address?): Array<Address> = addresses?.map { Address.Companion.from(it) }
    ?.filterNot { alreadyExists(it, newAddress) }
    ?.map {
      it.setToPrevious()
    }?.toTypedArray() ?: arrayOf()

  private fun alreadyExists(address: Address, newAddress: Address?): Boolean = address.copy(recordType = null) == newAddress?.copy(recordType = null)
}
