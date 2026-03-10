package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import java.time.LocalDate

data class CanonicalRecordView(
  val canonicalRecord: CanonicalRecord,
  val sentences: Set<LocalDate>,
)
