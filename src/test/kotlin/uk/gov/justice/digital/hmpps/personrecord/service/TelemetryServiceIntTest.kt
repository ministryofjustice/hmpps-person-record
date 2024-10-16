package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase

class TelemetryServiceIntTest : IntegrationTestBase() {

  @Test
  fun `should log CORRELATION_ID in telemetry events`() {
    val telemetryLogs = telemetryRepository.findAll()

    val correlationIdKey = EventKeys.CORRELATION_ID.name

    assertThat(telemetryLogs).anySatisfy { telemetry ->
      assertThat(telemetry.properties).contains(correlationIdKey)
    }
  }
}
