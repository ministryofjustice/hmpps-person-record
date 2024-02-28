package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Name(
  val forename: String? = null,
  val surname: String? = null,
  val otherNames: List<String>? = emptyList(),
)
