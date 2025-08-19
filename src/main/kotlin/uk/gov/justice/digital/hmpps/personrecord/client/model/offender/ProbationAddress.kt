package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import java.time.LocalDate

data class ProbationAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
)
