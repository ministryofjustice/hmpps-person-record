package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog

@Component
class EventLogService(
  private val eventLogRepository: EventLogRepository,
) {

  fun logEvent(eventLog: RecordEventLog): EventLogEntity = eventLogRepository.save(EventLogEntity.from(eventLog))
}
