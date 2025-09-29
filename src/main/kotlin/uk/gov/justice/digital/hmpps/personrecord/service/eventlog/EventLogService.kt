package uk.gov.justice.digital.hmpps.personrecord.service.eventlog

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog

@Component
class EventLogService(
  private val eventLogRepository: EventLogRepository,
  private val objectMapper: ObjectMapper,
) {

  fun logEvent(eventLog: RecordEventLog): EventLogEntity {
    val clusterComposition = eventLog.personKeyEntity?.clusterComposition ?: eventLog.personEntity.personKey?.clusterComposition
    return eventLogRepository.save(
      EventLogEntity.from(
        eventLog,
        clusterComposition?.let { objectMapper.writeValueAsString(clusterComposition) },
      ),
    )
  }
}
