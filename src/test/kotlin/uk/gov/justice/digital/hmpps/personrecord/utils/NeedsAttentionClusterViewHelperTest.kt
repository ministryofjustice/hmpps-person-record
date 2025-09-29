package uk.gov.justice.digital.hmpps.personrecord.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.AdminCluster
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster.SourceSystemComposition
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS

class NeedsAttentionClusterViewHelperTest {
  val commonPlatform = SourceSystemComposition(sourceSystem = COMMON_PLATFORM, count = 1)
  val delius = SourceSystemComposition(sourceSystem = DELIUS, count = 1)

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
}
