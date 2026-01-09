package uk.gov.justice.digital.hmpps.personrecord.telemetry

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.EnableAsync
import tools.jackson.databind.json.JsonMapper

@Configuration
@EnableAsync
@Profile("test")
class TelemetryTestConfig {

  @Bean
  fun telemetryClient(telemetryRepository: TelemetryTestRepository, jsonMapper: JsonMapper): TelemetryClient = OurTelemetryClient(telemetryRepository, jsonMapper)

  class OurTelemetryClient(private val telemetryRepository: TelemetryTestRepository, private val jsonMapper: JsonMapper) : TelemetryClient() {
    override fun trackEvent(
      event: String?,
      properties: MutableMap<String, String>?,
      metrics: MutableMap<String, Double>?,
    ) {
      val telemetry = TelemetryEntity(event = event, properties = jsonMapper.writeValueAsString(properties))
      telemetryRepository.saveAndFlush(telemetry)
    }
  }
}
