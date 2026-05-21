package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object AddressBuilder {

  fun buildAddresses(person: Person, personEntity: PersonEntity): List<AddressEntity> = person.addresses.mapNotNull { address ->
    address.existsIn(
      childEntities = personEntity.addresses,
      match = { ref, entity -> ref.matches(entity) },
      yes = { ref, entity -> entity.update(ref) },
      no = { AddressEntity.from(address) },
    )
  }

  // Once the final work around changing how we consume probation address
  // is done, this can be reverted back
  private fun Address.matches(entity: AddressEntity): Boolean {
    if (entity.deliusAddressId != null) {
      return entity.deliusAddressId == this.deliusAddressId
    }
    return this == Address.from(entity)
  }
}
