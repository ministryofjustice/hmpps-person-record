package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.extractSourceSystemId
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class UnmergeEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @TransactionalEventListener
  fun onUnmergeEvent(personUnmerged: PersonUnmerged) {
    publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECORD_UNMERGED, personUnmerged.reactivatedRecord))
    publisher.publishEvent(RecordEventLog.from(CPRLogEvents.CPR_RECORD_UPDATED, personUnmerged.unmergedRecord))
    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_RECORD_UNMERGED,
        mapOf(
          EventKeys.REACTIVATED_UUID to personUnmerged.reactivatedRecord.personKey?.personUUID.toString(),
          EventKeys.UNMERGED_UUID to personUnmerged.unmergedRecord.personKey?.personUUID.toString(),
          EventKeys.FROM_SOURCE_SYSTEM_ID to personUnmerged.unmergedRecord.extractSourceSystemId(),
          EventKeys.TO_SOURCE_SYSTEM_ID to personUnmerged.reactivatedRecord.extractSourceSystemId(),
          EventKeys.SOURCE_SYSTEM to personUnmerged.reactivatedRecord.sourceSystem.name,
        ),
      ),
    )
  }
}
