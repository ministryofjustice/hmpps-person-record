package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.libra

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  @JsonProperty("pcode")
  val postcode: String? = null,
)
