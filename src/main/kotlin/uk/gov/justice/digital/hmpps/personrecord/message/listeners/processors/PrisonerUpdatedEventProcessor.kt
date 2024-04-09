package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.service.PrisonerService

const val PRISONER_UPDATED = "prisoner-offender-search.prisoner.updated"

@Component(value = PRISONER_UPDATED)
class PrisonerUpdatedEventProcessor(
  val prisonerService: PrisonerService,
) : EventProcessor() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber!!
    log.debug("Entered processEvent with  $nomsNumber")
    prisonerService.processPrisonerDomainEvent(domainEvent)
  }
}
