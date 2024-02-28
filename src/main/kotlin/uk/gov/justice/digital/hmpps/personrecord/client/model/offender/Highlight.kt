package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Highlight(
  @JsonProperty("surname")
  val preferredName: List<String>? = emptyList(),
) {
  companion object
  fun getPreferredName(): String? {
    return preferredName?.joinToString { " " }
  }
}
