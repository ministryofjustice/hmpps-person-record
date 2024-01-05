package uk.gov.justice.digital.hmpps.personrecord.message.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import java.net.URI

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"

@Component(value = NEW_OFFENDER_CREATED)
class OffenderCreatedEventProcessor(
  val telemetryService: TelemetryService,
) : EventProcessor() {
  override fun processEvent(domainEvent: DomainEvent) {
    val offenderDetailUrl = domainEvent.detailUrl
    val path = URI.create(offenderDetailUrl).path
    val crn = domainEvent.personReference?.identifiers?.first { it.type == "CRN" }
    telemetryService.trackEvent(
      TelemetryEventType.DELIUS_RECORD_CREATION_RECEIVED,
      mapOf("CRN" to crn?.value),
    )
    LOG.debug("Entered processEvent with  urlR:$offenderDetailUrl")
  }
}
