package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

data class RecordClusterTelemetry(
  val telemetryEventType: TelemetryEventType,
  val cluster: PersonKeyEntity,
  val elementMap: Map<EventKeys, String?> = emptyMap(),
)
