package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  MESSAGE_ID,
  SOURCE_SYSTEM,
  EVENT_TYPE,
  RECORD_COUNT,
  UUID_COUNT,
  SEARCH_VERSION,
  UUID,
  QUERY,
  CLUSTER_SIZE,

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
}

@Component
class TelemetryService(private val telemetryClient: TelemetryClient) {

  fun trackPersonEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystem.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to person.crn,
      EventKeys.PRISON_NUMBER to person.prisonNumber,
    )
    trackEvent(eventType, identifierMap + elementMap)
  }

  fun trackPersonEvent(
    eventType: TelemetryEventType,
    personEntity: PersonEntity,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to personEntity.sourceSystem.name,
      EventKeys.DEFENDANT_ID to personEntity.defendantId,
      EventKeys.CRN to personEntity.crn,
      EventKeys.PRISON_NUMBER to personEntity.prisonNumber,
    )
    trackEvent(eventType, identifierMap + elementMap)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<EventKeys, String?>) {
    val transformedDimensions = customDimensions.entries.associate { it.key.name to it.value }
    log.debug("Sending telemetry event: ${eventType.name}")
    telemetryClient.trackEvent(eventType.eventName, transformedDimensions, null)
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
