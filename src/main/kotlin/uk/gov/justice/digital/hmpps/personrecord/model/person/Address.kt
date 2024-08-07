package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Address as PrisonerAddress

class Address(
  val postcode: String? = null,
  val fullAddress: String? = null,
) {
  companion object {
    fun from(address: PrisonerAddress): Address {
      return Address(
        postcode = address.postcode,
        fullAddress = address.fullAddress,
      )
    }

    fun fromList(addresses: List<PrisonerAddress>): List<Address> {
      return addresses.map { from(it) }
    }
  }
}
