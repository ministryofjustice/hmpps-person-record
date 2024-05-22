package uk.gov.justice.digital.hmpps.personrecord.telemetry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TelemetryTestRepository : JpaRepository<TelemetryEntity, Long> {

  fun findAllByEvent(event: String): List<TelemetryEntity>?
}
