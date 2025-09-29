package uk.gov.justice.digital.hmpps.personrecord.utils

import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS

object NeedsAttentionClusterViewHelper {
  fun process(clusters: List<AdminCluster>): List<AdminCluster> {
    val noneDelius = clusters.partition { it.recordComposition.any { it.sourceSystem != DELIUS && it.count > 0 } }
    val noneNomis = noneDelius.first.partition { it.recordComposition.any { it.sourceSystem != NOMIS && it.count > 0 } }

    return noneDelius.second + noneNomis.second + noneNomis.first
  }
}
