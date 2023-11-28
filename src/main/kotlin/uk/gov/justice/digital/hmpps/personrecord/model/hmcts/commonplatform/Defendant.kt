package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Defendant(
  val pncId: String? = null,
  val croNumber: String? = null,
  @Valid
  val personDefendant: PersonDefendant? = null,
)
