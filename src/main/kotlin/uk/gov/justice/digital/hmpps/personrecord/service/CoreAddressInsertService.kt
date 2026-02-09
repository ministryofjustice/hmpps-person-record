package uk.gov.justice.digital.hmpps.personrecord.service

import uk.gov.justice.digital.hmpps.personrecord.model.person.Address

class CoreAddressInsertService(
  personMatchAwareServiceDependencies: PersonMatchAwareServiceDependencies
) : PersonMatchAwareService<Address, Address>(
  personMatchAwareServiceDependencies
) {

  override fun process(input: Address): Address {

    // insert address then return addressEntity
    return Address()
  }
}