package uk.gov.justice.digital.hmpps.personrecord.message.processors.nomis

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.EventProcessor
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerDomainEventService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

const val PRISONER_CREATED = "prisoner-offender-search.prisoner.created"

@Component(value = PRISONER_CREATED)
class PrisonerCreatedEventProcessor(
  val prisonerDomainEventService: PrisonerDomainEventService,
  val telemetryService: TelemetryService,
) : EventProcessor() {

  override fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber
    telemetryService.trackEvent(
      TelemetryEventType.NOMIS_CREATE_MESSAGE_RECEIVED,
      mapOf("NOMS_NUMBER" to nomsNumber),
    )
    prisonerDomainEventService.processEvent(domainEvent)
  }
}
