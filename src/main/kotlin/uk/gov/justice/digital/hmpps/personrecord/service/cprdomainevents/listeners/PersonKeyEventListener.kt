package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Component
class PersonKeyEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @EventListener
  fun onPersonKeyCreated(personKeyCreated: PersonKeyCreated) {
    publisher.publishEvent(RecordPersonTelemetry(CPR_UUID_CREATED, personKeyCreated.personEntity, mapOf(EventKeys.UUID to personKeyCreated.personKeyEntity.personId.toString())))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_UUID_CREATED, personKeyCreated.personEntity, personKeyCreated.personKeyEntity))
  }

  @EventListener
  fun onPersonKeyFound(personKeyFound: PersonKeyFound) {
    publisher.publishEvent(
      RecordPersonTelemetry(
        CPR_CANDIDATE_RECORD_FOUND_UUID,
        personKeyFound.personEntity,
        mapOf(
          EventKeys.UUID to personKeyFound.personKeyEntity.personId?.toString(),
          EventKeys.CLUSTER_SIZE to personKeyFound.personKeyEntity.personEntities.size.toString(),
        ),
      ),
    )
  }
}
