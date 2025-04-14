package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import java.util.UUID

@Repository
interface EventLogRepository : JpaRepository<EventLogEntity, Long> {
  fun findByUuid(uuid: UUID): List<EventLogEntity>

  fun findBySourceSystemIdOrderByEventTimestampDesc(sourceSystemId: String): List<EventLogEntity>?
}
