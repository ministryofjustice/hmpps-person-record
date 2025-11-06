package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import java.time.LocalDate

data class PrisonSexualOrientation(
  val sexualOrientationCode: String,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val current: Boolean,
)
