package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

enum class EventKeys {
  // Message
  COMMON_PLATFORM,
  LIBRA,
  NOMIS,
  DELIUS,
  SOURCE_SYSTEM,

  // Identifiers
  CRN,
  PRISON_NUMBER,
  DEFENDANT_ID,
  C_ID,
  MATCH_ID,

  // Merge
  TO_UUID,
  FROM_UUID,
  FROM_SOURCE_SYSTEM_ID,
  TO_SOURCE_SYSTEM_ID,

  // Unmerge
  REACTIVATED_UUID,
  UNMERGED_UUID,

  // Matching
  HIGH_CONFIDENCE_COUNT,
  LOW_CONFIDENCE_COUNT,

  // Cluster
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

  fun trackClusterEvent(
    eventType: TelemetryEventType,
    cluster: PersonKeyEntity,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.UUID to cluster.personUUID.toString(),
      EventKeys.CLUSTER_SIZE to cluster.personEntities.size.toString(),
    )
    trackEvent(eventType, identifierMap + elementMap)
  }

  fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<EventKeys, String?>) {
    val transformedDimensions = customDimensions.entries.associate { it.key.name to it.value }
    telemetryClient.trackEvent(eventType.eventName, transformedDimensions, null)
  }
}
