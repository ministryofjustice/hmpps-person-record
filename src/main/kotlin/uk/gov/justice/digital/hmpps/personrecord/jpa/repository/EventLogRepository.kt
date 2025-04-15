package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents

@Repository
interface EventLogRepository : JpaRepository<EventLogEntity, Long> {

  fun findAllByEventTypeAndSourceSystemIdOrderByEventTimestampDesc(eventType: CPRLogEvents, sourceSystemId: String): List<EventLogEntity>?
}
