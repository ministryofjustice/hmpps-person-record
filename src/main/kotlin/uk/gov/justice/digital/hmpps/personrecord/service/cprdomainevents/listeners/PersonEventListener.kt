package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class PersonEventListener(
  private val telemetryService: TelemetryService,
  private val eventLogService: EventLogService,
) {

  @EventListener
  fun handlePersonCreated(personCreated: PersonCreated) {
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_CREATED, personCreated.personEntity)
    eventLogService.logEvent(personCreated.personEntity, CPRLogEvents.CPR_RECORD_CREATED)
  }

  @EventListener
  fun handlePersonUpdated(personUpdated: PersonUpdated) {
    telemetryService.trackPersonEvent(TelemetryEventType.CPR_RECORD_UPDATED, personUpdated.personEntity)
    when {
      personUpdated.matchingFieldsHaveChanged -> eventLogService.logEvent(personUpdated.personEntity, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }
}
