package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DefendantAlias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleName: String? = null,
)
