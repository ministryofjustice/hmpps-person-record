package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

data class SysconReligionResponseBody(
  val prisonNumber: String,
  val religionMappings: List<SysconReligionMapping>,
) {
  companion object {
    fun from(prisonId: String, cprReligionIdByNomisId: Map<String, String>): SysconReligionResponseBody = SysconReligionResponseBody(
      prisonNumber = prisonId,
      religionMappings = cprReligionIdByNomisId.map { SysconReligionMapping(it.key, it.value) },
    )
  }
}

data class SysconReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String,
)
