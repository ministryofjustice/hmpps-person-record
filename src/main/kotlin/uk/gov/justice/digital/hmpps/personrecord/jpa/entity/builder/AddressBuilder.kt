package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

object AddressBuilder {

  fun buildAddresses(person: Person, personEntity: PersonEntity) = person.addresses.mapNotNull { address ->
    address.existsIn(
      childEntities = personEntity.addresses,
      match = { incomingAddress, existing ->
        incomingAddress.matches(existing) ||
          (incomingAddress.deliusAddressId != null && existing.deliusAddressId != null && incomingAddress.deliusAddressId == existing.deliusAddressId)
      },
      yes = { ref, entity -> entity.update(ref) },
      no = { AddressEntity.from(address) },
    )
  }

  private fun Address.matches(entity: AddressEntity): Boolean = this == Address.from(entity)
}
