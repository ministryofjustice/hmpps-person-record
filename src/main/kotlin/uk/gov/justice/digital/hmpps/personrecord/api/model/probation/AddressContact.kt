package uk.gov.justice.digital.hmpps.personrecord.api.model.probation

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

@Schema(name = "ProbationCreateAddressContact")
data class AddressContact(
  @Schema(description = "The address contact type", example = "HOME", required = true)
  val typeCode: ContactType,
  @Schema(description = "The address contact value", example = "+44 20 7946 0000", required = true)
  val value: String,
  @Schema(description = "The address contact extension", example = "1234")
  val extension: String? = null,
)
