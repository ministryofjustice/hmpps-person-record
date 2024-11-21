package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLoggingEntity

@Repository
interface EventLoggingRepository : JpaRepository<EventLoggingEntity, Long> {

  fun findBySourceSystemId(sourceSystemId: String): EventLoggingEntity?

  @Query("SELECT * FROM event_logging WHERE source_system_id = :crn ORDER BY event_timestamp DESC LIMIT 1", nativeQuery = true)
  fun findLatestEventByCrn(@Param("crn") crn: String): EventLoggingEntity?
}
