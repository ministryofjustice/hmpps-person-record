package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Address as OffenderAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Address as PrisonerAddress

class Address(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
) {
  companion object {
    fun from(address: PrisonerAddress): Address {
      return Address(
        postcode = address.postcode,
        fullAddress = address.fullAddress,
        startDate = address.startDate,
        noFixedAbode = address.noFixedAbode,
      )
    }

    fun from(address: OffenderAddress): Address {
      return Address(
        noFixedAbode = address.noFixedAbode,
        startDate = address.startDate,
        endDate = address.endDate,
        postcode = address.postcode,
        fullAddress = address.fullAddress,
      )
    }

    fun fromPrisonerAddressList(addresses: List<PrisonerAddress>): List<Address> {
      return addresses.map { from(it) }
    }

    fun fromOffenderAddressList(addresses: List<OffenderAddress>): List<Address> {
      return addresses.map { from(it) }
    }
    fun convertEntityToAddress(addressEntity: AddressEntity): Address {
      return Address(
        postcode = addressEntity.postcode,
        fullAddress = addressEntity.fullAddress,
        startDate = addressEntity.startDate,
        noFixedAbode = addressEntity.noFixedAbode,
      )
    }
  }
}
