package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity

data class CanonicalAddress(
  @Schema(description = "CPR address Id", example = "ec4c7479-218c-4f11-a02d-edd749820679")
  val cprAddressId: String,
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
  @Schema(description = "Person address building Name", example = "Main Building")
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
  @Schema(description = "Person address country")
  val country: CanonicalCountry,
  @Schema(description = "Person address uprn", example = "100120991537")
  val uprn: String? = null,
  @Schema(description = "Person address status")
  val status: CanonicalAddressStatus,
  @Schema(description = "Person address comment", example = "United Kingdom")
  val comment: String? = null,
  @Schema(description = "List of person address usages")
  val usages: List<CanonicalAddressUsage> = emptyList(),
) {

  companion object {
    fun from(addressEntity: AddressEntity): CanonicalAddress = CanonicalAddress(
      cprAddressId = addressEntity.updateId!!.toString(),
      noFixedAbode = addressEntity.noFixedAbode,
      startDate = addressEntity.startDate?.toString(),
      endDate = addressEntity.endDate?.toString(),
      postcode = addressEntity.postcode,
      subBuildingName = addressEntity.subBuildingName,
      buildingName = addressEntity.buildingName,
      buildingNumber = addressEntity.buildingNumber,
      thoroughfareName = addressEntity.thoroughfareName,
      dependentLocality = addressEntity.dependentLocality,
      postTown = addressEntity.postTown,
      county = addressEntity.county,
      country = CanonicalCountry.from(addressEntity.countryCode),
      uprn = addressEntity.uprn,
      status = CanonicalAddressStatus.from(addressEntity.statusCode),
      comment = addressEntity.comment,
      usages = CanonicalAddressUsage.fromAddressUsageEntityList(addressEntity.usages),
    )

    fun fromAddressEntityList(addressEntity: List<AddressEntity>): List<CanonicalAddress> = addressEntity.map { from(it) }
  }
}
