package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCases(

  val totalPages: Int,
  val first: Boolean,
  val last: Boolean,
  @JsonProperty("content")
  val cases: List<DeliusOffenderDetail>,
)
