package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster


data class AdminCluster(
  val uuid: String,
  val recordComposition: SourceSystemComposition,
)

data class SourceSystemComposition(
  val nomis: Int,
  val delius: Int,
  val commonPlatform: Int,
  val libra: Int,
)