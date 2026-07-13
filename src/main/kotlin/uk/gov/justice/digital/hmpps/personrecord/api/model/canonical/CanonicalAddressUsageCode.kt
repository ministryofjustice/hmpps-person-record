package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

data class CanonicalAddressUsageCode(
  @Schema(
    description = "Address usage code",
    example = "CURFEW",
    allowableValues = ["CARE", "CURFEW", "DAP", "DBA", "DBH", "DNF", "DOH", "DPH", "DSH", "DST", "DUT", "HDC", "HDC2", "HOME", "HOSP", "HOST", "OTHER", "RECEP", "RELEASE", "RES", "ROTL", "A02", "A16", "A10", "A11", "A17", "A07B", "A07A", "A14", "A13", "A08A", "A08C", "A08", "A01A", "A15", "A12", "A01C", "A01D", "A04", "A03", "UNKNOWN"],
  )
  val code: String? = null,
  @Schema(description = "Address usage description", example = "Curfew Order")
  val description: String? = null,
) {
  companion object {

    fun from(addressUsageCode: AddressUsageCode?) = CanonicalAddressUsageCode(
      code = addressUsageCode?.name,
      description = addressUsageCode?.description,
    )
  }
}
