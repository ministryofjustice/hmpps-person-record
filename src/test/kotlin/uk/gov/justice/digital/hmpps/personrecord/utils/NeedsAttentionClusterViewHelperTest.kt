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
  val delius = SourceSystemComposition(sourceSystem = DELIUS, count = 1)
  val libra = SourceSystemComposition(sourceSystem = LIBRA, count = 1)
  val nomis = SourceSystemComposition(sourceSystem = NOMIS, count = 1)


  @Test
  fun `common platform and LIBRA records come last`() {
    val commonPlatformCluster = AdminCluster(
      uuid = "",
      recordComposition = listOf(
        commonPlatform,
      ),
    )
    val libraCluster = AdminCluster(
      uuid = "",
      recordComposition = listOf(
        libra,
      ),
    )
    val clusters: List<AdminCluster> = listOf(
      commonPlatformCluster,
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          delius,
        ),
      ),
      libraCluster,
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          nomis,
        ),
      ),
    )
    val result = NeedsAttentionClusterViewHelper.process(clusters)
    val firstTwo = listOf(result[0], result[1])
    assertThat(firstTwo).doesNotContain(commonPlatformCluster)
    assertThat(firstTwo).doesNotContain(libraCluster)
  }

  @Test
  fun `should give priority to clusters with delius records over commonPlatform records`() {
    val clusters: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
        ),
      ),
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          delius,
        ),
      ),
    )

    val result = NeedsAttentionClusterViewHelper.process(clusters)

    val expected: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          delius,
        ),
      ),
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
        ),
      ),
    )

    assertThat(result).isEqualTo(expected)
  }

  @Test
  fun `should give priority to clusters with only delius records over clusters with both delius and commonPlatform records`() {
    val clusters: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
          delius,
        ),
      ),
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          delius,
        ),
      ),
    )

    val result = NeedsAttentionClusterViewHelper.process(clusters)

    val expected: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          delius,
        ),
      ),
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

  @Test
  fun `should give priority to clusters with only nomis records over clusters with commonPlatform records`() {
    val clusters: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
        ),
      ),
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          nomis,
        ),
      ),
    )

    val result = NeedsAttentionClusterViewHelper.process(clusters)

    val expected: List<AdminCluster> = listOf(
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          nomis,
        ),
      ),
      AdminCluster(
        uuid = "",
        recordComposition = listOf(
          commonPlatform,
        ),
      ),
    )

    assertThat(result).isEqualTo(expected)
  }
}
