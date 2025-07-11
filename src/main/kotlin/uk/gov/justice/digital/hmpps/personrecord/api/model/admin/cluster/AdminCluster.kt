package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

data class AdminCluster(
  val uuid: String,
  val recordComposition: List<SourceSystemComposition>,
)

data class SourceSystemComposition(
  val sourceSystem: SourceSystemType,
  val count: Int,
)
