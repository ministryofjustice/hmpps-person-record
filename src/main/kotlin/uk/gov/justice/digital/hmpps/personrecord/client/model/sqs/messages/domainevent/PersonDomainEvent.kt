package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonDomainEvent(
  @JsonProperty("eventType") val eventType: String,
  @JsonProperty("version") val version: Int = 1,
  @JsonProperty("description") val description: String,
  @JsonProperty("detailUrl") val detailUrl: String? = null,
  @JsonProperty("occurredAt") val occurredAt: String,
  @JsonProperty("additionalInformation") val additionalInformation: Map<String, String>? = null,
  @JsonProperty("personReference") val personReference: PersonReference? = null,
)
