package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

data class SysconReligionResponseBody(
  val prisonNumber: String,
  val religionMappings: List<SysconReligionMapping>,
) {
  companion object {
    fun from(prisonNumber: String, cprReligionIdByNomisId: Map<String, String>): SysconReligionResponseBody = SysconReligionResponseBody(
      prisonNumber = prisonNumber,
      religionMappings = cprReligionIdByNomisId.map { SysconReligionMapping(it.key, it.value) },
    )
  }
}

// TODO: remove and share PrisonReligionMapping
data class SysconReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
