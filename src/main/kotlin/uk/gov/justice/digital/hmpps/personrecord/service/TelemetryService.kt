package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  MESSAGE_ID,
  SOURCE_SYSTEM,
  EVENT_TYPE,
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
    val transformedDimensions = customDimensions.entries.associate { it.key.name to it.value }

    telemetryClient.trackEvent(eventType.eventName, transformedDimensions, null)
  }
}
