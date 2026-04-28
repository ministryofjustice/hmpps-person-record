package uk.gov.justice.digital.hmpps.personrecord.api.model.probation

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

@Schema(name = "ProbationCreateAddressUsage")
data class AddressUsage(
  @Schema(description = "The address usage code", example = "DSH")
  val usageCode: AddressUsageCode,
  @Schema(description = "Is the address active", example = "true")
  val isActive: Boolean,
)
