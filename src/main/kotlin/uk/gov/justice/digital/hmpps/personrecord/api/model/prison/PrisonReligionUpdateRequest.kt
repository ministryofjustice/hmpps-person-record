package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import java.time.LocalDate
import java.time.LocalDateTime

data class PrisonReligionUpdateRequest(
  val nomisReligionId: String,
  val current: Boolean,
  val modifyUserId: String,
  val modifyDateTime: LocalDateTime,
  val endDate: LocalDate? = null,
  val comments: String? = null,
  val verified: Boolean? = null,
)
