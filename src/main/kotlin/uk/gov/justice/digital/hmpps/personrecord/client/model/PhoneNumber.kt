package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PhoneNumber(
  val number: String? = null,
  val type: String? = null,
)
