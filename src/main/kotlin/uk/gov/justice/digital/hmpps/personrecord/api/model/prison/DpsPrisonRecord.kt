package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import com.fasterxml.jackson.annotation.JsonUnwrapped
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

data class DpsPrisonRecord(
  @JsonUnwrapped
  val record: CanonicalRecord,
  val religionHistory: List<PrisonReligion>,
  val prisonAlias: List<DpsPrisonAlias>,
) {
  companion object {
    fun from(personEntity: PersonEntity, prisonReligionEntities: List<PrisonReligionEntity>): DpsPrisonRecord = DpsPrisonRecord(
      record = CanonicalRecord.from(personEntity),
      religionHistory = prisonReligionEntities.map { PrisonReligion.from(it) },
      prisonAlias = personEntity.pseudonyms.sortedWith(compareBy({ it.nameType != NameType.PRIMARY }, { it.id })).map { DpsPrisonAlias.from(it) },
    )
  }
}
