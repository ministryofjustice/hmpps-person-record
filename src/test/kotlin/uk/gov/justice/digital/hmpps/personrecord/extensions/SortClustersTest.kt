package uk.gov.justice.digital.hmpps.personrecord.extensions

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

class SortClustersTest {
  val commonPlatform = SourceSystemComposition(sourceSystem = SourceSystemType.COMMON_PLATFORM, count = 1)
  val commonPlatformCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      commonPlatform,
    ),
  )
  val delius = SourceSystemComposition(sourceSystem = SourceSystemType.DELIUS, count = 1)
  val deliusCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      delius,
    ),
  )
  val libraCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      SourceSystemComposition(sourceSystem = SourceSystemType.LIBRA, count = 1),
    ),
  )
  val nomisCluster = AdminCluster(
    uuid = "",
    recordComposition = listOf(
      SourceSystemComposition(sourceSystem = SourceSystemType.NOMIS, count = 1),
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
    val result = clusters.sort()
    val firstTwo = listOf(result[0], result[1])
    Assertions.assertThat(firstTwo).doesNotContain(commonPlatformCluster)
    Assertions.assertThat(firstTwo).doesNotContain(libraCluster)
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

    val result = clusters.sort()

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

    Assertions.assertThat(result).isEqualTo(expected)
  }
}
