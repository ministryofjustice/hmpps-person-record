package uk.gov.justice.digital.hmpps.personrecord.extensions

import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA

fun List<AdminCluster>.sort(): List<AdminCluster> {
  val court = this.partition { cluster ->
    cluster.recordComposition.any {
      it.hasCourtRecords()
    }
  }
  return court.second + court.first
}

private val courtSourceSystems = listOf(
  LIBRA,
  COMMON_PLATFORM,
)

private fun SourceSystemComposition.hasCourtRecords() = courtSourceSystems.contains(this.sourceSystem) && this.count > 0
