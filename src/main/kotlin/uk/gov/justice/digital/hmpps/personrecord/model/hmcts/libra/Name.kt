package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Name(
  val title: String? = null,
  val forename1: String? = null,
  val forename2: String? = null,
  val forename3: String? = null,
  val surname: String? = null,
)
