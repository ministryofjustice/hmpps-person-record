package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AdditionalInformation(
  @JsonProperty("categoriesChanged")
  val categoriesChanged: List<String>? = emptyList(),

  @JsonProperty("nomsNumber")
  val prisonNumber: String? = null,

  @JsonProperty("sourceCRN")
  val sourceCrn: String? = null,

  @JsonProperty("targetCRN")
  val targetCrn: String? = null,
)
