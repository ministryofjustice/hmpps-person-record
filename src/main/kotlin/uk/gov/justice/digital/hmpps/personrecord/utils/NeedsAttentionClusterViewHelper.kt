package uk.gov.justice.digital.hmpps.personrecord.utils

import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS

object NeedsAttentionClusterViewHelper {
  fun process(clusters: List<AdminCluster>): List<AdminCluster> {
    val partition = clusters.partition { it.recordComposition.any { it.sourceSystem != DELIUS && it.count > 0 } }
    return partition.second + partition.first
  }
}
