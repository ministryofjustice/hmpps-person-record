package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenderProfile(
  val ethnicity: String? = null,
  val nationality: String? = null,
  val secondaryNationality: String? =  null,
)
