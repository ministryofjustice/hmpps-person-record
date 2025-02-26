package uk.gov.justice.digital.hmpps.personrecord.service.type

import org.junit.jupiter.api.Test

class TelemetryEventTypeTest {
  @Test
  fun `output telemetry event types`() {
    TelemetryEventType.entries.map { it.eventName }.sorted().forEach { println(it) }
  }
}
