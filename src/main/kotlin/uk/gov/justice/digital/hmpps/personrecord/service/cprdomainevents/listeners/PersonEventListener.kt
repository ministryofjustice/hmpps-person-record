package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.recluster.Recluster
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class PersonEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @EventListener
  fun onPersonCreated(personCreated: PersonCreated) {
    publisher.publishEvent(RecordPersonTelemetry(TelemetryEventType.CPR_RECORD_CREATED, personCreated.personEntity))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, personCreated.personEntity))
  }

  @EventListener
  fun onPersonUpdated(personUpdated: PersonUpdated) {
    publisher.publishEvent(RecordPersonTelemetry(TelemetryEventType.CPR_RECORD_UPDATED, personUpdated.personEntity))
    when {
      personUpdated.matchingFieldsHaveChanged -> {
        publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UPDATED, personUpdated.personEntity))
        personUpdated.personEntity.personKey?.let {
          publisher.publishEvent(Recluster(it, changedRecord = personUpdated.personEntity))
        }
      }
    }
  }
}
