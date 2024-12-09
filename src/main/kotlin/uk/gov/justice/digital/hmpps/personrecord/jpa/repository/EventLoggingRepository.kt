package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity

@Repository
interface EventLoggingRepository : JpaRepository<EventLoggingEntity, Long> {
  fun findFirstBySourceSystemIdOrderByEventTimestampDesc(sourceSystemId: String): EventLoggingEntity?

  fun findFirstByUuidOrderByEventTimestampDesc(uuid: String): EventLoggingEntity?
}
