package uk.gov.justice.digital.hmpps.personrecord.message.processors.cpr

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.Recluster
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MESSAGE_RECEIVED
import java.util.UUID

@Component
class ReclusterEventProcessor(
  private val telemetryService: TelemetryService,
  private val reclusterService: ReclusterService,
) {

  fun processEvent(reclusterEvent: Recluster) {
    telemetryService.trackEvent(
      CPR_RECLUSTER_MESSAGE_RECEIVED,
      mapOf(EventKeys.UUID to reclusterEvent.uuid),
    )
    reclusterService.recluster(UUID.fromString(reclusterEvent.uuid))
  }
}
