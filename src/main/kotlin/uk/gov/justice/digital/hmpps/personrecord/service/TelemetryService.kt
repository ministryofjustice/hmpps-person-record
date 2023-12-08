package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    log.debug("Sending telemetry event ${ eventType.eventName} ")
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }
}
