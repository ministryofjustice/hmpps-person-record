package uk.gov.justice.digital.hmpps.personrecord.utils

import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA

fun sortClusters(clusters: List<AdminCluster>): List<AdminCluster> {
  val court = clusters.partition { cluster ->
    cluster.recordComposition.any {
      listOf(
        LIBRA,
        COMMON_PLATFORM,
      ).contains(it.sourceSystem) &&
        it.count > 0
    }
  }
  return court.second + court.first
}
