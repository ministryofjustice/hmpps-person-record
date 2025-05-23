package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry

@Component
class TelemetryEventListener(
  private val telemetryService: TelemetryService,
) {

  @Async
  @EventListener
  @TransactionalEventListener
  fun onTelemetryEvent(telemetry: RecordTelemetry) {
    telemetryService.trackEvent(
      telemetry.telemetryEventType,
      telemetry.elementMap,
    )
  }

  @Async
  @EventListener
  @TransactionalEventListener
  fun onPersonTelemetryEvent(personTelemetryEvent: RecordPersonTelemetry) {
    telemetryService.trackPersonEvent(
      personTelemetryEvent.telemetryEventType,
      personTelemetryEvent.personEntity,
      personTelemetryEvent.elementMap,
    )
  }

  @Async
  @EventListener
  @TransactionalEventListener
  fun onClusterTelemetryEvent(clusterTelemetry: RecordClusterTelemetry) {
    telemetryService.trackClusterEvent(
      clusterTelemetry.telemetryEventType,
      clusterTelemetry.cluster,
      clusterTelemetry.elementMap,
    )
  }
}
