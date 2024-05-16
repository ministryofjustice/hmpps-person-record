package uk.gov.justice.digital.hmpps.personrecord.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.MultiNodeTestBase

internal class FeatureFlagTest : MultiNodeTestBase() {

  private var featureFlag = getBean("featureFlag", FeatureFlag::class.java)

  @Test
  fun `should return true when hmcts sqs feature is enabled`() {
    assertThat(featureFlag.isHmctsSQSEnabled()).isTrue
  }

  @Test
  fun `should return true when delius search feature is enabled`() {
    assertThat(featureFlag.isDeliusSearchEnabled()).isTrue
  }

  @Test
  fun `should return false when delius domain event feature switch is enabled`() {
    assertThat(featureFlag.isDeliusDomainEventSQSDisabled()).isFalse
  }
}
