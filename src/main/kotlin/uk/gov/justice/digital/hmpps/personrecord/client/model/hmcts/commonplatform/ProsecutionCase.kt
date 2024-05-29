package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProsecutionCase(
  @NotNull
  @Valid
  val defendants: List<Defendant>,
)
