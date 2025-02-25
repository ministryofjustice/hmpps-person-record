package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class CanonicalAddress(
  val noFixedAbode: String? = "",
  val startDate: String? = "",
  val endDate: String? = "",
  val postcode: String? = "",
  val subBuildingName: String? = "",
  val buildingName: String? = "",
  val buildingNumber: String? = "",
  val thoroughfareName: String? = "",
  val dependentLocality: String? = "",
  val postTown: String? = "",
  val county: String? = "",
  val country: String? = "",
  val uprn: String? = "",
) {
  companion object {

    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      postcode = addressEntity.postcode,
      startDate = addressEntity.startDate?.toString(),
      endDate = addressEntity.endDate?.toString(),
      noFixedAbode = addressEntity.noFixedAbode.toString(),
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
    )
    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = addressEntity.map { from(it) }
  }
}
