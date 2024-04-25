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
  fun `should return true when delius search feature is enabled`() {
    assertThat(featureFlag.isDeliusSearchEnabled()).isTrue
  }

  @Test
  fun `should return true when delius domain event feature switch is enabled`() {
    assertThat(featureFlag.isDeliusDomainEventSQSEnabled()).isTrue
  }
}
