package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
@JsonIgnoreProperties(ignoreUnknown = true)
data class Hearing(
  val id: String? = null,

  @NotEmpty
  @Valid
  val prosecutionCases: List<ProsecutionCase>,
)
