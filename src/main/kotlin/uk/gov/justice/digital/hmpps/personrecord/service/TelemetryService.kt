package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  // Message
  MESSAGE_ID,
  SOURCE_SYSTEM,
  EVENT_TYPE,

  // Identifiers
  CRN,
  PRISON_NUMBER,
  DEFENDANT_ID,
  C_ID,
  MATCH_ID,

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
  HIGH_CONFIDENCE_COUNT,
  LOW_CONFIDENCE_COUNT,

  // Cluster
  CLUSTER_COMPOSITION,
  RECORD_COUNT,
  UUID_COUNT,
  UUID,
  CLUSTER_SIZE,
}

@Component
class TelemetryService(private val telemetryClient: TelemetryClient) {

  fun trackPersonEvent(
    eventType: TelemetryEventType,
    personEntity: PersonEntity,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to personEntity.sourceSystem.name,
      EventKeys.MATCH_ID to personEntity.matchId.toString(),
      EventKeys.DEFENDANT_ID to personEntity.defendantId,
      EventKeys.C_ID to personEntity.cId,
      EventKeys.CRN to personEntity.crn,
      EventKeys.PRISON_NUMBER to personEntity.prisonNumber,
    )
    trackEvent(eventType, identifierMap + elementMap)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<EventKeys, String?>) {
    val transformedDimensions = customDimensions.entries.associate { it.key.name to it.value }
    telemetryClient.trackEvent(eventType.eventName, transformedDimensions, null)
  }
}
