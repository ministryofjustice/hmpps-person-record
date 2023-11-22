package uk.gov.justice.digital.hmpps.personrecord.model.libra

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LibraName(
  val title: String? = null,
  val forename1: String? = null,
  val forename2: String? = null,
  val forename3: String? = null,
  val surname: String? = null,
)
