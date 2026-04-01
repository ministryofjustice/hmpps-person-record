package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

data class PrisonReligionSaveResponse(
  val prisonNumber: String,
  val religionMappings: PrisonReligionMapping,
)

data class PrisonReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
