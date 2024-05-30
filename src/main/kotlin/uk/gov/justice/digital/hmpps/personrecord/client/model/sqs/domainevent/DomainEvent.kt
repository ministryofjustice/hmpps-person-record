package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEvent @JsonCreator constructor(
  @JsonProperty("eventType") val eventType: String,
  @JsonProperty("detailUrl") val detailUrl: String,
  @JsonProperty("personReference") val personReference: PersonReference? = null,
  @JsonProperty("additionalInformation") val additionalInformation: AdditionalInformation? = null,
)
