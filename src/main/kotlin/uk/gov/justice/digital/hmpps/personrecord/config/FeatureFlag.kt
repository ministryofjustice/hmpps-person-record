package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class FeatureFlag(private val environment: Environment) {

  fun isHmctsSQSDisabled(): Boolean {
    return environment.getProperty("feature.flags.enable-hmcts-sqs", Boolean::class.java, true) == false
  }

  fun isDeliusDomainEventSQSDisabled(): Boolean {
    return environment.getProperty("feature.flags.enable-delius-domain-event-sqs", Boolean::class.java, true) == false
  }
}
