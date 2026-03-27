package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PrisonCanonicalRecord(
  @JsonUnwrapped
  val record: CanonicalRecord,
) {
  companion object {
    fun from(personEntity: PersonEntity): PrisonCanonicalRecord = PrisonCanonicalRecord(CanonicalRecord.from(personEntity))
  }
}
