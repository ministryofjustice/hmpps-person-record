package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class CanonicalAddress(
  @Schema(description = "Person no fixed abode", examples = ["false", "true", "null"])
  val noFixedAbode: Boolean? = null,
  @Schema(description = "Person address start date", example = "2020-02-26")
  val startDate: String? = null,
  @Schema(description = "Person address end date", example = "2023-07-15")
  val endDate: String? = null,
  @Schema(description = "Person address postcode", example = "SW1H 9AJ")
  val postcode: String? = null,
  @Schema(description = "Person address sub building name", example = "Sub building 2")
  val subBuildingName: String? = null,
  @Schema(description = "Person address building uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Name", example = "Main Building")
  val buildingName: String? = null,
  @Schema(description = "Person address building number", example = "102")
  val buildingNumber: String? = null,
  @Schema(description = "Person address thoroughfareName", example = "Petty France")
  val thoroughfareName: String? = null,
  @Schema(description = "Person address dependentLocality", example = "Westminster")
  val dependentLocality: String? = null,
  @Schema(description = "Person address postTown", example = "London")
  val postTown: String? = null,
  @Schema(description = "Person address county", example = "Greater London")
  val county: String? = null,
  @Schema(description = "Person address country", example = "United Kingdom")
  val country: String? = null,
  @Schema(description = "Person address uprn", example = "100120991537")
  val uprn: String? = null,
) {
  companion object {

    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      postcode = addressEntity.postcode,
      startDate = addressEntity.startDate?.toString(),
      endDate = addressEntity.endDate?.toString(),
      noFixedAbode = addressEntity.noFixedAbode,
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
    )
    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = addressEntity.map { from(it) }
  }
}
