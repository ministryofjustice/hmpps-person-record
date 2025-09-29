package uk.gov.justice.digital.hmpps.personrecord.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS

class NeedsAttentionClusterViewHelperTest {
  val commonPlatform = SourceSystemComposition(sourceSystem = COMMON_PLATFORM, count = 1)
  val commonPlatformCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      commonPlatform,
    ),
  )
  val delius = SourceSystemComposition(sourceSystem = DELIUS, count = 1)
  val deliusCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      delius,
    ),
  )
  val libraCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      SourceSystemComposition(sourceSystem = LIBRA, count = 1),
    ),
  )
  val nomisCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      SourceSystemComposition(sourceSystem = NOMIS, count = 1),
    ),
  )

  @Test
  fun `common platform and LIBRA records come last`() {
    val clusters: List<AdminCluster> = listOf(
      commonPlatformCluster,
      deliusCluster,
      libraCluster,
      nomisCluster,
    )
    val result = sortClusters(clusters)
    val firstTwo = listOf(result[0], result[1])
    assertThat(firstTwo).doesNotContain(commonPlatformCluster)
    assertThat(firstTwo).doesNotContain(libraCluster)
  }

  @Test
  fun `should put clusters with no court records before clusters with court records`() {
    val clusters: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
          delius,
        ),
      ),
      deliusCluster,
    )

    val result = sortClusters(clusters)

    val expected: List<AdminCluster> = listOf(
      deliusCluster,
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
          delius,
        ),
      ),
    )

    assertThat(result).isEqualTo(expected)
  }
}
