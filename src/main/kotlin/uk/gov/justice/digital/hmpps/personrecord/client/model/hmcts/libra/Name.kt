package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.libra

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Name(
  val title: String? = null,
  @JsonProperty("forename1")
  val firstName: String? = null,
  @JsonProperty("surname")
  val lastName: String? = null,

  val forename2: String? = null,
  val forename3: String? = null,
)
