package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

data class PrisonReligionResponseBody(
  val prisonerNumber: String,
  val religionMappings: PrisonReligionMapping,
) {
  companion object {
    fun from(prisonerNumber: String, prisonReligionMapping: PrisonReligionMapping) = PrisonReligionResponseBody(
      prisonerNumber = prisonerNumber,
      religionMappings = prisonReligionMapping,
    )
  }
}

data class PrisonReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
