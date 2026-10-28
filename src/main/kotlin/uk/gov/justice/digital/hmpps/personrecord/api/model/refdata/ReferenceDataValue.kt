package uk.gov.justice.digital.hmpps.personrecord.api.model.refdata

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.CurrentReferenceData
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceData

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReferenceDataValue(
  @Schema(description = "The reference data code", example = "M")
  val code: String,
  @Schema(description = "The reference data description", example = "Male")
  val description: String,
  @Schema(description = "Whether the reference data value is current. Only present for reference data types that track this", example = "true")
  val current: Boolean? = null,
) {
  companion object {
    fun from(value: ReferenceData): ReferenceDataValue = ReferenceDataValue(
      code = (value as Enum<*>).name,
      description = value.description,
      current = (value as? CurrentReferenceData)?.current ?: true,
    )
  }
}
