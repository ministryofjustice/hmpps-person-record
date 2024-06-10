package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  MESSAGE_ID,
  SOURCE_SYSTEM,
  EVENT_TYPE,
  RECORD_COUNT,
  SEARCH_VERSION,

  // Identifiers
  CRN,
  PRISON_NUMBER,
  PNC,
  CRO,
  DEFENDANT_ID,
}

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<EventKeys, String?>) {
    log.debug("Sending telemetry event ${ eventType.eventName} ")
    val transformedDimensions: Map<String, String?> = customDimensions.entries.associate { it.key.name to it.value }
    telemetryClient.trackEvent(eventType.eventName, transformedDimensions, null)
  }
}
