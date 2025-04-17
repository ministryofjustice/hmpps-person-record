package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry

import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

data class RecordTelemetry(
  val telemetryEventType: TelemetryEventType,
  val elementMap: Map<EventKeys, String?> = emptyMap(),
)
