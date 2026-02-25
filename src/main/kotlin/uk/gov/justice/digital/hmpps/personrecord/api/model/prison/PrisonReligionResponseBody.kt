package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import java.util.UUID

data class PrisonReligionResponseBody(
  val prisonerNumber: String,
  val religionMappings: PrisonReligionMapping,
) {
  companion object {
    fun from(prisonerNumber: String, nomisReligionId: String, cprReligionId: UUID?) = PrisonReligionResponseBody(
      prisonerNumber = prisonerNumber,
      religionMappings = PrisonReligionMapping(
        nomisReligionId = nomisReligionId,
        cprReligionId = cprReligionId.toString(),
      ),
    )
  }
}

data class PrisonReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
