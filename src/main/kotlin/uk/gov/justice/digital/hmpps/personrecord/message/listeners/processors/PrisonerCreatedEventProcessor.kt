package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerDomainEventService

const val PRISONER_CREATED = "prisoner-offender-search.prisoner.created"

@Component(value = PRISONER_CREATED)
class PrisonerCreatedEventProcessor(
  val prisonerDomainEventService: PrisonerDomainEventService,
) : EventProcessor() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber!!
    log.debug("Entered processEvent with  $nomsNumber")
    prisonerDomainEventService.processEvent(domainEvent)
  }
}
