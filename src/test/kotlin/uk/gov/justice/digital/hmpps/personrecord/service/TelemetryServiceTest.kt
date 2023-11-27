package uk.gov.justice.digital.hmpps.personrecord.service

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@ExtendWith(MockitoExtension::class)
class TelemetryServiceTest{

  @Mock
  private lateinit var telemetryClient: TelemetryClient

  @InjectMocks
  lateinit var telemetryService: TelemetryService
  @Test
  fun `should track event when provided with custom dimensions and event type`() {
    // Given
    val customDimensions = mapOf("key" to "value")

    // When
    telemetryService.trackEvent(TelemetryEventType.NEW_CASE_EXACT_MATCH, customDimensions)

    // Then
    verify(telemetryClient).trackEvent("CprNewCaseExactMatch", customDimensions, null)
  }
}