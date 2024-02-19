package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
  val home: String? = null,
  val mobile: String? = null,
  val work: String? = null,
  val primaryEmail: String? = null,
)
