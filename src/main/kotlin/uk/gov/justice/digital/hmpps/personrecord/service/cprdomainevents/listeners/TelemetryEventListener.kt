package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry

@Component
class TelemetryEventListener(
  private val telemetryService: TelemetryService,
) {

  @Async
  @TransactionalEventListener
  fun onTelemetryEvent(personTelemetryEvent: RecordPersonTelemetry) {
    telemetryService.trackPersonEvent(
      personTelemetryEvent.telemetryEventType,
      personTelemetryEvent.personEntity,
    )
  }
}
