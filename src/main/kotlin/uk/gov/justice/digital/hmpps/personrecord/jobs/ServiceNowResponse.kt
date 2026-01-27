package uk.gov.justice.digital.hmpps.personrecord.jobs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceNowResult(
  @JsonProperty("request_number") val requestNumber: String,
)

data class ServiceNowResponse(
  val result: ServiceNowResult,
)
