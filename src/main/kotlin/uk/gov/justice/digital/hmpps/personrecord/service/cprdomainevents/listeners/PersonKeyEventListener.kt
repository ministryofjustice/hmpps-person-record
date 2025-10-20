package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys.UUID
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.DeliusMergeRequest
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordPersonTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_DELIUS_MERGE_REQUEST_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_DELETED

@Component
class PersonKeyEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @EventListener
  fun onPersonKeyCreated(personKeyCreated: PersonKeyCreated) {
    publisher.publishEvent(RecordPersonTelemetry(CPR_UUID_CREATED, personKeyCreated.personEntity, mapOf(UUID to personKeyCreated.personKeyEntity.personUUID.toString())))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_UUID_CREATED, personKeyCreated.personEntity, EventLogClusterDetail.from(personKeyCreated.personKeyEntity)))
  }

  @EventListener
  fun onDeliusMergeRequest(deliusMergeRequest: DeliusMergeRequest) {
    publisher.publishEvent(RecordPersonTelemetry(
      CPR_DELIUS_MERGE_REQUEST_CREATED, deliusMergeRequest.personEntity, mapOf(
      UUID to deliusMergeRequest.personKeyEntity.personUUID.toString())))
  }

  @EventListener
  fun onPersonKeyFound(personKeyFound: PersonKeyFound) {
    publisher.publishEvent(
      RecordPersonTelemetry(
        CPR_CANDIDATE_RECORD_FOUND_UUID,
        personKeyFound.personEntity,
        mapOf(
          UUID to personKeyFound.personKeyEntity.personUUID?.toString(),
          EventKeys.CLUSTER_SIZE to personKeyFound.personKeyEntity.personEntities.size.toString(),
        ),
      ),
    )
  }

  @EventListener
  fun onPersonKeyDeleted(personKeyDeleted: PersonKeyDeleted) {
    publisher.publishEvent(RecordPersonTelemetry(CPR_UUID_DELETED, personKeyDeleted.personEntity, mapOf(UUID to personKeyDeleted.personKeyEntity.personUUID.toString())))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_UUID_DELETED, personKeyDeleted.personEntity))
  }
}
