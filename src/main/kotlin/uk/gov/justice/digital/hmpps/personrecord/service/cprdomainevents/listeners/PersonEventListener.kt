package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.UUID
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonProcessingCompleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_DELIUS_MERGE_REQUEST_CREATED

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
    if (personUpdated.matchingFieldsHaveChanged) {
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UPDATED, personUpdated.personEntity))
    }
  }

  @EventListener
  fun onPersonDeleted(personDeleted: PersonDeleted) {
    publisher.publishEvent(RecordPersonTelemetry(TelemetryEventType.CPR_RECORD_DELETED, personDeleted.personEntity, mapOf(EventKeys.UUID to personDeleted.personEntity.personKey?.personUUID?.toString())))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_DELETED, personDeleted.personEntity))
  }

  @EventListener
  fun onPersonProcessingCompleted(personProcessingCompleted: PersonProcessingCompleted) {
    personProcessingCompleted.personEntity.personKey?.let { personKey ->
      if (personKey.hasMoreThanOneProbationRecord()) {
        publisher.publishEvent(
          RecordPersonTelemetry(
            CPR_DELIUS_MERGE_REQUEST_CREATED,
            personProcessingCompleted.personEntity,
            mapOf(
              UUID to personKey.personUUID.toString(),
              EventKeys.CRNS to personKey.crns().joinToString(),
            ),
          ),
        )
      }
    }
  }

  private fun PersonKeyEntity.hasMoreThanOneProbationRecord(): Boolean = this.crns().size > 1
  private fun PersonKeyEntity.crns(): List<String?> = this.personEntities.filter { person -> person.sourceSystem == DELIUS }.map { it.crn }
}
