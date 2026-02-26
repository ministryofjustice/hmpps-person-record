package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

data class PrisonReligionResponseBody(
  val prisonerNumber: String,
  val religionMappings: PrisonReligionMapping,
) {
  companion object {
    fun from(prisonerNumber: String, nomisReligionId: String, cprReligionId: String) = PrisonReligionResponseBody(
      prisonerNumber = prisonerNumber,
      religionMappings = PrisonReligionMapping(
        nomisReligionId = nomisReligionId,
        cprReligionId = cprReligionId,
      ),
    )
  }
}

data class PrisonReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
