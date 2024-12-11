package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLoggingRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService

@Configuration
@Profile("test")
class EventLoggingTestConfig {

  @Bean
  fun eventLoggingService(eventLoggingRepository: EventLoggingRepository, telemetryClient: TelemetryClient, objectMapper: ObjectMapper): EventLoggingService {
    return EventLoggingService(eventLoggingRepository, telemetryClient, objectMapper)
  }
}
