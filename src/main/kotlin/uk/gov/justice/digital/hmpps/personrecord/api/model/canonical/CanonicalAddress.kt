package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class CanonicalAddress(
  @Schema(description = "Person no fixed abode", example = "True")
  val noFixedAbode: String? = "",
  @Schema(description = "Person address start date", example = "02/02/2020")
  val startDate: String? = "",
  @Schema(description = "Person address end date", example = "04/04/2023")
  val endDate: String? = "",
  @Schema(description = "Person address postcode", example = "B10 1EJ")
  val postcode: String? = "",
  @Schema(description = "Person address sub building name", example = "Sub building 2")
  val subBuildingName: String? = "",
  @Schema(description = "Person address building Name", example = "Main Building")
  val buildingName: String? = "",
  @Schema(description = "Person address building number", example = "5")
  val buildingNumber: String? = "",
  @Schema(description = "Person address thoroughfareName", example = "One Street")
  val thoroughfareName: String? = "",
  @Schema(description = "Person address dependentLocality", example = "Town One")
  val dependentLocality: String? = "",
  @Schema(description = "Person address postTown", example = "PostTown")
  val postTown: String? = "",
  @Schema(description = "Person address county", example = "West Midlands")
  val county: String? = "",
  @Schema(description = "Person address country", example = "United Kingdom")
  val country: String? = "",
  @Schema(description = "Person address uprn", example = "100120991537")
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
