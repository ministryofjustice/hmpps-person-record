package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonSexualOrientationEntity
import java.util.*

@Repository
interface PrisonSexualOrientationRepository : JpaRepository<PrisonSexualOrientationEntity, Long> {
  fun findByCprSexualOrientationId(cprSexualOrientationId: UUID): PrisonSexualOrientationEntity?
}
