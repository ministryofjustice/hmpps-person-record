package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import java.time.LocalDateTime

data class PrisonReligionUpdateRequest(
  val modifyUserId: String,
  val modifyDateTime: LocalDateTime,
  val comments: String? = null,
)
