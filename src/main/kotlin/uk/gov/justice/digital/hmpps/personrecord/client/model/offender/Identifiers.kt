package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifiers(
  val defendantId: String? = null,
  val crn: String? = null,
  val pnc: String? = null,
  val cro: String? = null,
  @JsonProperty("prisonerNumber")
  val prisonNumber: String? = null,
  @JsonProperty("ni")
  val nationalInsuranceNumber: String? = null,
)
