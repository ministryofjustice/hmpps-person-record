package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

data class PrisonCanonicalRecord(
  @JsonUnwrapped
  val record: CanonicalRecord,
  val religionHistory: List<PrisonReligionGet>,
  val prisonReferences: List<PrisonReference>,
) {
  companion object {
    fun from(
      personEntity: PersonEntity,
      prisonReligionEntities: List<PrisonReligionEntity>,
      prisonReferences: List<PrisonReference>,
    ): PrisonCanonicalRecord = PrisonCanonicalRecord(
      record = CanonicalRecord.from(personEntity),
      religionHistory = prisonReligionEntities.map { PrisonReligionGet.from(it) },
      prisonReferences = prisonReferences,
    )
  }
}
