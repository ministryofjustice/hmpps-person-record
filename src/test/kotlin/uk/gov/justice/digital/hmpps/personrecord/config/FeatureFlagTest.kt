package uk.gov.justice.digital.hmpps.personrecord.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase

internal class FeatureFlagTest : IntegrationTestBase() {

  @Autowired
  private lateinit var featureFlag: FeatureFlag

  @Test
  fun `should return true when hmcts sqs feature is enabled`() {
    assertThat(featureFlag.isHmctsSQSEnabled()).isTrue
  }

  @Test
  fun `should return false when delius search feature is disabled`() {
    assertThat(featureFlag.isDeliusSearchEnabled()).isTrue()
  }

  @Test
  fun `should return true when nomis search feature switch is not present`() {
    assertThat(featureFlag.isNomisSearchEnabled()).isFalse()
  }
}
