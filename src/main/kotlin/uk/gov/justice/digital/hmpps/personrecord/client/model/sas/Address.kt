package uk.gov.justice.digital.hmpps.personrecord.client.model.sas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  val postcode: String? = null,
)
