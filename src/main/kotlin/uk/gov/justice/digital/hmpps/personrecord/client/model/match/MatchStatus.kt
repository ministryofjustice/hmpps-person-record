package uk.gov.justice.digital.hmpps.personrecord.client.model.match

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchStatus(
  val status: String? = null,
)
