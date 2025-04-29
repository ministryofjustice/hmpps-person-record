package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService

@Component
class EventLogListener(
  val eventLogService: EventLogService,
) {

  @Async
  @EventListener
  @TransactionalEventListener
  fun onEventLogEvent(eventLog: RecordEventLog) {
    eventLogService.logEvent(eventLog)
  }
}
