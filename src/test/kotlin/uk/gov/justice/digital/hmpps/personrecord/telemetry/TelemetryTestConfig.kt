package uk.gov.justice.digital.hmpps.personrecord.telemetry

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableAsync
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableAsync
@Profile("test")
class TelemetryTestConfig {

  @Bean
  fun telemetryClient(telemetryRepository: TelemetryTestRepository, objectMapper: ObjectMapper): TelemetryClient = OurTelemetryClient(telemetryRepository, objectMapper)

  class OurTelemetryClient(private val telemetryRepository: TelemetryTestRepository, private val objectMapper: ObjectMapper) : TelemetryClient() {
    override fun trackEvent(
      event: String?,
      properties: MutableMap<String, String>?,
      metrics: MutableMap<String, Double>?,
    ) {
      val telemetry = TelemetryEntity(event = event, properties = objectMapper.writeValueAsString(properties))
      telemetryRepository.saveAndFlush(telemetry)
    }
  }
}
