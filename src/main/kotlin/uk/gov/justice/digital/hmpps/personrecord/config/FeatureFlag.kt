package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class FeatureFlag(private val environment: Environment) {

  fun isHmctsSQSEnabled(): Boolean {
    return environment.getProperty("feature.flags.enable-hmcts-sqs", Boolean::class.java, true)
  }

  fun isDeliusSearchEnabled(): Boolean {
    return environment.getProperty("feature.flags.enable-delius-search", Boolean::class.java, true)
  }

  fun isDeliusDomainEventSQSDisabled(): Boolean {
    return environment.getProperty("feature.flags.enable-delius-domain-event-sqs", Boolean::class.java, true) == false
  }
}
