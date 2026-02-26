package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

data class PrisonReligionResponseBody(
  val prisonNumber: String,
  val religionMappings: PrisonReligionMapping,
) {
  companion object {
    fun from(prisonNumber: String, prisonReligionMapping: PrisonReligionMapping) = PrisonReligionResponseBody(
      prisonNumber = prisonNumber,
      religionMappings = prisonReligionMapping,
    )
  }
}

data class PrisonReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
