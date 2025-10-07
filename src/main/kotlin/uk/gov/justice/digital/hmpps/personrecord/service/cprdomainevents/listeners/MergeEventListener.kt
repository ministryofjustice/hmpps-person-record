package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.EventLogClusterDetail
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.ClusterMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class MergeEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @TransactionalEventListener
  fun onClusterMergeEvent(clusterMerged: ClusterMerged) {
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_UUID_MERGED, clusterMerged.from, EventLogClusterDetail.from(clusterMerged.fromMergedPersonKeyEntity)))
    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_UUID_MERGED,
        mapOf(
          EventKeys.FROM_UUID to clusterMerged.fromMergedPersonKeyEntity.personUUID.toString(),
          EventKeys.TO_UUID to clusterMerged.to.personKey?.personUUID.toString(),
          EventKeys.SOURCE_SYSTEM to clusterMerged.to.sourceSystem.name,
        ),
      ),
    )
  }

  @TransactionalEventListener
  fun onMergeEvent(personMerged: PersonMerged) {
    personMerged.from?.let { publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_MERGED, it, personMerged.fromClusterDetail)) }
    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_RECORD_MERGED,
        mapOf(
          EventKeys.FROM_SOURCE_SYSTEM_ID to personMerged.from?.extractSourceSystemId(),
          EventKeys.TO_SOURCE_SYSTEM_ID to personMerged.to.extractSourceSystemId(),
          EventKeys.SOURCE_SYSTEM to personMerged.to.sourceSystem.name,
        ),
      ),
    )
  }
}
