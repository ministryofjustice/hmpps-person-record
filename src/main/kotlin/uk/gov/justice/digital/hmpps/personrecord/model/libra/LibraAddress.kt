package uk.gov.justice.digital.hmpps.personrecord.model.libra

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LibraAddress(
  val line1: String? = null,
  val line2: String? = null,
  val line3: String? = null,
  val line4: String? = null,
  val line5: String? = null,
  val pcode: String? = null,
)
