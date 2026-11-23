package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import java.util.UUID

@Repository
interface PseudonymRepository : JpaRepository<PseudonymEntity, Long> {
  fun findByUpdateId(updateId: UUID): PseudonymEntity?
}
