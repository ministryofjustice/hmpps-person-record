package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity

data class PrisonReligionReadResponse(
  val prisonNumber: String,
  val religion: PrisonReligion,
) {
  companion object {
    fun from(prisonNumber: String, prisonReligionEntity: PrisonReligionEntity): PrisonReligionReadResponse = PrisonReligionReadResponse(
      prisonNumber = prisonNumber,
      religion = PrisonReligion.from(prisonReligionEntity),
    )
  }
}
