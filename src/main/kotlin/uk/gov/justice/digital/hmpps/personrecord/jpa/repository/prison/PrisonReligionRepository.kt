package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.prison.PrisonReligionEntity
import java.util.UUID

@Repository
interface PrisonReligionRepository : JpaRepository<PrisonReligionEntity, Long> {
  fun findByCprReligionId(cprReligionId: UUID): PrisonReligionEntity?
}
