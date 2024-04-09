package uk.gov.justice.digital.hmpps.personrecord.message.listeners.processors

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent

const val PRISONER_UPDATED = "probation-case.engagement.updated"

@Component(value = PRISONER_UPDATED)
class PrisonerUpdatedEventProcessor() : EventProcessor() {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  override fun processEvent(domainEvent: DomainEvent) {
    val nomsNumber = domainEvent.additionalInformation?.nomsNumber!!
    log.debug("Entered processEvent with  $nomsNumber")
  }
}
