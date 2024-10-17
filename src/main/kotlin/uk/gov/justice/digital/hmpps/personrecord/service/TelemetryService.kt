package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  MESSAGE_ID,
  SOURCE_SYSTEM,
  EVENT_TYPE,
  CORRELATION_ID,
  RECORD_COUNT,
  SEARCH_VERSION,
  UUID,
  QUERY,

  // Identifiers
  CRN,
  PRISON_NUMBER,
  PNC,
  CRO,
  DEFENDANT_ID,

  // Merge
  RECORD_TYPE,

  SOURCE_CRN,
  TARGET_CRN,
  TO_UUID,
  FROM_UUID,
  SOURCE_PRISON_NUMBER,
  TARGET_PRISON_NUMBER,

  // Unmerge
  REACTIVATED_UUID,
  UNMERGED_UUID,
  REACTIVATED_CRN,
  UNMERGED_CRN,

  // Matching
  PROBABILITY_SCORE,
  HIGH_CONFIDENCE_COUNT,
  LOW_CONFIDENCE_COUNT,
  IS_ABOVE_SELF_MATCH_THRESHOLD,

  FIFO,
  HEARING_ID,
}

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun trackPersonEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to person.crn,
      EventKeys.PRISON_NUMBER to person.prisonNumber,
    )
    trackEvent(eventType, identifierMap + elementMap)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<EventKeys, String?>) {
    log.debug("Sending telemetry event ${eventType.eventName} ")

    val correlationId = telemetryClient.context.operation.id

    val updatedDimensions = customDimensions.entries.associate { it.key.name to it.value } + mapOf("CORRELATION_ID" to correlationId)

    log.debug("Test : setting correlation id to $correlationId")
    log.debug("TelemetryService : setting correlation id to $correlationId")


    telemetryClient.trackEvent(eventType.eventName, updatedDimensions, null)
  }
}
