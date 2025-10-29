package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder.address

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

object AddressBuilder {

  fun buildAddresses(person: Person, personEntity: PersonEntity): List<AddressEntity> = when (personEntity.sourceSystem) {
    SourceSystemType.COMMON_PLATFORM -> CommonPlatformAddressBuilder.build(person, personEntity)
    else -> person.addresses.build(personEntity)
  }

  private fun List<Address>.build(personEntity: PersonEntity) = this.mapNotNull { address ->
    address.existsIn(
      childEntities = personEntity.addresses,
      match = { ref, entity -> ref.matches(entity) },
      yes = { it },
      no = { AddressEntity.from(address) },
    )
  }

  private fun Address.matches(entity: AddressEntity): Boolean = this == Address.from(entity)
}
