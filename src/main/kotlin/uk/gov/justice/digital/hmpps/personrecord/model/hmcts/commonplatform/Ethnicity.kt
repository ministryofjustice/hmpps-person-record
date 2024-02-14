package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Ethnicity(
  val observedEthnicityDescription: String? = null,
  val selfDefinedEthnicityDescription: String? = null,
)
