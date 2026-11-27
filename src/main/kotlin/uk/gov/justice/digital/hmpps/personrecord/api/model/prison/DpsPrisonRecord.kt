package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonPseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

data class DpsPrisonRecord(
  @JsonUnwrapped
  val record: CanonicalRecord,
  val religionHistory: List<PrisonReligion>,
  val prisonAliases: List<DpsPrisonAlias>,
) {
  companion object {
    fun from(
      personEntity: PersonEntity,
      prisonReligionEntities: List<PrisonReligionEntity>,
      prisonPseudonymEntities: List<PrisonPseudonymEntity>,
    ): DpsPrisonRecord = DpsPrisonRecord(
      record = CanonicalRecord.from(personEntity),
      religionHistory = prisonReligionEntities.map { PrisonReligion.from(it) },
      prisonAliases = prisonPseudonymEntities.associateBy { it.id }.let { prisonPseudonyms ->
        personEntity.pseudonyms.map { DpsPrisonAlias.from(it, prisonPseudonyms[it.id]) }
      },
    )
  }
}
