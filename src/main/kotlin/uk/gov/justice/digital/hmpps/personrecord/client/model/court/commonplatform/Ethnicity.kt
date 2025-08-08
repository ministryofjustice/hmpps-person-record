package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ethnicity(
  val observedEthnicityDescription: String? = null,
  val selfDefinedEthnicityDescription: String? = null,
  val selfDefinedEthnicityCode: String? = null,
)
