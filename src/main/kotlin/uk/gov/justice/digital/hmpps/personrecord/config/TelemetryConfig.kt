package uk.gov.justice.digital.hmpps.personrecord.config

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.lang.NonNull
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
@Profile("!test")
class TelemetryConfig {

  @Bean
  @Conditional(AppInsightKeyAbsentCondition::class)
  fun telemetryClient(): TelemetryClient = TelemetryClient()

  private class AppInsightKeyAbsentCondition : Condition {
    override fun matches(@NonNull context: ConditionContext, @NonNull metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty("application.insights.ikey").isNullOrEmpty()
  }
}
