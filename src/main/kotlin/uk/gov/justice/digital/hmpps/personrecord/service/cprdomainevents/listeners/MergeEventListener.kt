package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class MergeEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @TransactionalEventListener
  fun onMergeEvent(personMerged: PersonMerged) {
    personMerged.from?.let { publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_MERGED, it, personMerged.fromClusterDetail)) }
    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_RECORD_MERGED,
        mapOf(
          EventKeys.FROM_SOURCE_SYSTEM_ID to personMerged.from?.extractSourceSystemId(),
          EventKeys.FROM_UUID to personMerged.fromClusterDetail.personUUID.toString(),
          EventKeys.TO_SOURCE_SYSTEM_ID to personMerged.to.extractSourceSystemId(),
          EventKeys.TO_UUID to personMerged.to.personKey?.personUUID.toString(),
          EventKeys.SOURCE_SYSTEM to personMerged.to.sourceSystem.name,
        ),
      ),
    )
  }
}
