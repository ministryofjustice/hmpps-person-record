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
  val subBuildingName: String? = null,
  val buildingName: String? = null,
  val buildingNumber: String? = null,
  val thoroughfareName: String? = null,
  val dependentLocality: String? = null,
  val postTown: String? = null,
  val county: String? = null,
  val country: String? = null,
  val uprn: String? = null,

) {
  companion object {
    fun from(address: PrisonerAddress): Address = Address(
      postcode = address.postcode,
      fullAddress = address.fullAddress,
      startDate = address.startDate,
      noFixedAbode = address.noFixedAbode,
    )

    fun from(address: OffenderAddress): Address = Address(
      noFixedAbode = address.noFixedAbode,
      startDate = address.startDate,
      endDate = address.endDate,
      postcode = address.postcode,
      fullAddress = address.fullAddress,
    )

    fun fromPrisonerAddressList(addresses: List<PrisonerAddress>): List<Address> = addresses.map { from(it) }

    fun fromOffenderAddressList(addresses: List<OffenderAddress>): List<Address> = addresses.map { from(it) }
    fun from(addressEntity: AddressEntity): Address = Address(
      postcode = addressEntity.postcode,
      fullAddress = addressEntity.fullAddress,
      startDate = addressEntity.startDate,
      noFixedAbode = addressEntity.noFixedAbode,
      subBuildingName = addressEntity.subBuildingName,
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
      county = addressEntity.county,
      country = addressEntity.country,
      uprn = addressEntity.uprn,
    )
  }
}
