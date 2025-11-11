package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonDisabilityStatusEntity
import java.util.UUID

@Repository
interface PrisonDisabilityStatusRepository : JpaRepository<PrisonDisabilityStatusEntity, Long> {
  fun findByCprDisabilityStatusId(cprDisabilityStatusId: UUID): PrisonDisabilityStatusEntity?
}
