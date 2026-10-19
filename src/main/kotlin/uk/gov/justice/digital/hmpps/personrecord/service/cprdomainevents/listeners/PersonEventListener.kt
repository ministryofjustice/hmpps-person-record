package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.IS_OVERRIDE_MARKER_DELETE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.UUID
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
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
    if (personUpdated.matchingFieldsHaveChanged()) {
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UPDATED, personUpdated.personEntity))
    }
  }

  @EventListener
  fun onPersonDeleted(personDeleted: PersonDeleted) {
    publisher.publishEvent(
      RecordPersonTelemetry(
        TelemetryEventType.CPR_RECORD_DELETED,
        personDeleted.personEntity,
        mapOf(
          UUID to personDeleted.cluster?.personUUID?.toString(),
          IS_OVERRIDE_MARKER_DELETE to if (personDeleted.personEntity.overrideMarker != null) "true" else "false",
        ),
      ),
    )
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_DELETED, personDeleted.personEntity, clusterDetail = EventLogClusterDetail.from(personDeleted.cluster)))
  }
}
