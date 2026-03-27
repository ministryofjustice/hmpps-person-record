package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

data class DpsPrisonRecord(
  @JsonUnwrapped
  val record: CanonicalRecord,
  val religionHistory: List<PrisonReligionGet>,
  val prisonPseudonyms: List<PrisonAlias>,
) {
  companion object {
    fun from(
      personEntity: PersonEntity,
      prisonReligionEntities: List<PrisonReligionEntity>,
      prisonAliases: List<PrisonAlias>,
    ): DpsPrisonRecord = DpsPrisonRecord(
      record = CanonicalRecord.from(personEntity),
      religionHistory = prisonReligionEntities.map { PrisonReligionGet.from(it) },
      prisonPseudonyms = prisonAliases,
    )
  }
}
