package uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.EventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerDomainEventService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"

@Component(value = PRISONER_UPDATED)
class PrisonerUpdatedEventProcessor(
  val prisonerDomainEventService: PrisonerDomainEventService,
  val telemetryService: TelemetryService,
) : EventProcessor() {

  override fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber
    telemetryService.trackEvent(
      TelemetryEventType.NOMIS_UPDATE_MESSAGE_RECEIVED,
      mapOf("NOMS_NUMBER" to nomsNumber),
    )
    prisonerDomainEventService.processEvent(domainEvent)
  }
}
