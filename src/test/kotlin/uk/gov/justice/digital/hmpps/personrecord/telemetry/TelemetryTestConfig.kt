package uk.gov.justice.digital.hmpps.personrecord.telemetry

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.UUID

@Configuration
@Profile("test")
class TelemetryTestConfig {

  @Bean
  fun telemetryClient(telemetryRepository: TelemetryTestRepository, objectMapper: ObjectMapper): TelemetryClient {
    return OurTelemetryClient(telemetryRepository, objectMapper)
  }

  class OurTelemetryClient(private val telemetryRepository: TelemetryTestRepository, private val objectMapper: ObjectMapper) : TelemetryClient() {
    val testCorrelation: String = UUID.randomUUID().toString()

    override fun trackEvent(
      event: String?,
      properties: MutableMap<String, String>?,
      metrics: MutableMap<String, Double>?,
    ) {
      // val correlationId = UUID.randomUUID().toString()
      val updatedProperties = properties?.toMutableMap() ?: mutableMapOf()
      updatedProperties["CORRELATION_ID"] = testCorrelation

      val telemetry = TelemetryEntity(event = event, properties = objectMapper.writeValueAsString(updatedProperties))

      telemetryRepository.saveAndFlush(telemetry)
    }
  }
}
