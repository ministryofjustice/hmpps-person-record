package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.res

data class SysconReligionResponseBody(
  val prisonerId: String,
  val religionMappings: List<SysconReligionMapping>,
) {
  companion object {
    fun from(prisonerId: String, cprReligionIdByNomisId: Map<String, String>): SysconReligionResponseBody {
      return SysconReligionResponseBody(
        prisonerId = prisonerId,
        religionMappings = cprReligionIdByNomisId.map { SysconReligionMapping(it.key, it.value) }
      )
    }
  }
}

data class SysconReligionMapping(
  val nomisReligionId: String,
  val cprReligionId: String
)