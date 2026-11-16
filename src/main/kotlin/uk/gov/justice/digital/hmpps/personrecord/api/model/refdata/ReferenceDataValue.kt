package uk.gov.justice.digital.hmpps.personrecord.api.model.refdata

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceData
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReferenceDataValue(
  @Schema(description = "The reference data code", example = "M")
  val code: String,
  @Schema(description = "The reference data description", example = "Male")
  val description: String,
  @Schema(description = "Whether the reference data value is current", example = "true")
  val current: Boolean? = null,
  @Schema(description = "Domains in which this reference data value is applicable")
  val applicableTo: Set<ReferenceDataDomain>,
) {
  companion object {
    fun from(value: ReferenceData): ReferenceDataValue = ReferenceDataValue(
      code = (value as Enum<*>).name,
      description = value.description,
      current = value.current,
      applicableTo = value.applicableTo,
    )
  }
}
