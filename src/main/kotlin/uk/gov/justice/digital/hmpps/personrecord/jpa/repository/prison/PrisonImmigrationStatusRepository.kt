package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonImmigrationStatusEntity
import java.util.UUID

@Repository
interface PrisonImmigrationStatusRepository : JpaRepository<PrisonImmigrationStatusEntity, Long> {
  fun findByCprImmigrationStatusId(cprImmigrationStatusId: UUID): PrisonImmigrationStatusEntity?
}
