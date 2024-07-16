package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonKey(
  @JsonProperty("type") val type: String,
  @JsonProperty("value") val value: String,
)
