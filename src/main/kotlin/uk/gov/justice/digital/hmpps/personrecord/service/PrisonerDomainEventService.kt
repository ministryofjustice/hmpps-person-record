package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent

@Service
class PrisonerDomainEventService(
  val telemetryService: TelemetryService,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processEvent(domainEvent: DomainEvent) {
    log.debug("Processing event with noms number ${domainEvent.additionalInformation?.nomsNumber}")
    // implement latter
  }
}
