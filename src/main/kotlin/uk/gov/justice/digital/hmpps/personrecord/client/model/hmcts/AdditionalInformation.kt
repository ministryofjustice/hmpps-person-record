package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalInformation(
  val categoriesChanged: List<String>? = emptyList(),
  @JsonProperty("nomsNumber")
  val prisonNumber: String,
)
