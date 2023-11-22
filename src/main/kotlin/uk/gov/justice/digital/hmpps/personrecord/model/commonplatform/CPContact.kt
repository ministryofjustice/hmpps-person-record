package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class CPContact {
  val home: String? = null
  val mobile: String? = null
  val work: String? = null
}
